package ro.fiismart.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ro.fiismart.ai.client.GeminiClient;
import ro.fiismart.ai.client.GeminiStreamChunk;
import ro.fiismart.chat.dto.ToolDispatchContext;
import ro.fiismart.chat.dto.request.RouteContextDTO;
import ro.fiismart.chat.model.ChatMessage;
import ro.fiismart.chat.model.ChatSession;
import ro.fiismart.chat.model.ToolCall;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrates one chat turn end-to-end:
 *
 * <ol>
 *   <li>Build the system prompt via {@link ChatContextBuilder} and serialize
 *       the trimmed message window.</li>
 *   <li>Open a Gemini stream with the tool declarations attached.</li>
 *   <li>Forward text deltas to the client as {@code event: token}.</li>
 *   <li>On a function-call chunk, dispatch through {@link ChatToolHandler},
 *       emit {@code event: tool_call} + {@code event: tool_result}, and
 *       (best-effort) continue the conversation with a follow-up Gemini
 *       call that has the tool result folded into the prompt.</li>
 *   <li>On {@code Done}: persist the assistant message, auto-title the
 *       session if it was still default, emit {@code event: done},
 *       complete the emitter.</li>
 * </ol>
 *
 * <p>Gemini stream rounds are hard-capped at {@link #MAX_STREAM_ITERATIONS}
 * per user turn (currently 2 → one re-prompt allowed after a tool call)
 * to defend against a model that keeps calling tools in a loop.
 *
 * <p>Heartbeat: a single shared {@link ScheduledExecutorService} pings
 * every 15s. Per-stream worker thread runs the blocking Gemini call so the
 * Tomcat request thread is not held hostage.
 */
@Service
public class GeminiChatService {

    private static final Logger log = LoggerFactory.getLogger(GeminiChatService.class);

    /** ~10 minutes. Long enough for tool-loops; short enough that orphaned
     * emitters get reclaimed within a sensible window. */
    public static final long SSE_TIMEOUT_MS = 10L * 60L * 1000L;

    public static final long HEARTBEAT_INTERVAL_SECONDS = 15L;

    /**
     * Soft cap on the number of Gemini stream rounds per user turn. With
     * the current value of 2 we allow at most ONE re-prompt after a tool
     * call: the first stream may end in a function-call, we dispatch the
     * tool, and then we re-stream once more with the tool result folded
     * into the prompt to get a final natural-language reply. That second
     * stream is the last one — even if it also produces a tool call, we
     * do NOT loop again. Increase this if the UX needs deeper chains.
     */
    static final int MAX_STREAM_ITERATIONS = 2;

    /** Default title replaced after the first user message. Must match
     *  {@link ChatService#DEFAULT_TITLE}. */
    private static final int TITLE_FROM_USER_MAX_LEN = 60;

    private final GeminiClient geminiClient;
    private final ChatContextBuilder contextBuilder;
    private final ChatToolHandler toolHandler;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    /**
     * Shared scheduler for SSE heartbeats. Daemon threads — JVM shutdown
     * is not blocked by idle pings.
     *
     * <p>Pool size scales with the available CPUs (with a floor of 4) so
     * that the third+ concurrent SSE stream still gets reliable
     * heartbeat ticks. With only 2 threads, a slow ping would block the
     * pool and idle proxies would tear down healthy streams.</p>
     */
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newScheduledThreadPool(
                    Math.max(4, Runtime.getRuntime().availableProcessors()),
                    r -> {
                        Thread t = new Thread(r, "chat-sse-heartbeat");
                        t.setDaemon(true);
                        return t;
                    });

    /**
     * Shut the heartbeat pool down on application shutdown so we don't
     * leak threads on hot redeploys or test-context restarts.
     */
    @PreDestroy
    void shutdownHeartbeat() {
        heartbeatScheduler.shutdownNow();
    }

    public GeminiChatService(GeminiClient geminiClient,
                             ChatContextBuilder contextBuilder,
                             ChatToolHandler toolHandler,
                             ChatService chatService,
                             ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.contextBuilder = contextBuilder;
        this.toolHandler = toolHandler;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    /**
     * Streams the assistant's reply to {@code emitter}. Must be invoked
     * on a worker thread — the call blocks until Gemini's stream finishes
     * (or is cancelled).
     *
     * <p>{@code session} is already-persisted state including the just-
     * appended user message — this method does not append it again.
     */
    public void streamResponse(ChatSession session,
                               RouteContextDTO routeContext,
                               String userId,
                               SseEmitter emitter) {

        AtomicBoolean finished = new AtomicBoolean(false);
        GeminiClient.StreamCancelHandle cancel = new GeminiClient.StreamCancelHandle();

        // If `send` throws, the emitter is already dead — cancel the
        // schedule rather than re-failing every interval and uselessly
        // tying up a scheduler slot until the outer shutdown runs.
        AtomicReference<ScheduledFuture<?>> heartbeatRef = new AtomicReference<>();
        ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (finished.get()) return;
            try {
                emitter.send(SseEmitter.event().name("ping").data("{}"));
            } catch (Exception e) {
                log.debug("Chat heartbeat send failed: {}", e.getClass().getSimpleName());
                ScheduledFuture<?> self = heartbeatRef.get();
                if (self != null) self.cancel(false);
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        heartbeatRef.set(heartbeat);

        Runnable shutdown = () -> {
            if (finished.compareAndSet(false, true)) {
                cancel.cancel();
                heartbeat.cancel(false);
            }
        };
        emitter.onCompletion(shutdown);
        emitter.onTimeout(() -> {
            log.info("Chat SSE emitter timed out session={} user={}", session.getId(), userId);
            shutdown.run();
        });
        emitter.onError(t -> {
            log.warn("Chat SSE emitter error session={}: {}",
                    session.getId(), t.getClass().getSimpleName());
            shutdown.run();
        });

        try {
            String systemPrompt = contextBuilder.buildSystemPrompt(userId, routeContext);
            List<Map<String, Object>> tools = buildToolDeclarations();

            StringBuilder accumulated = new StringBuilder();
            List<ToolCall> capturedToolCalls = new ArrayList<>();

            // Iteration loop: each pass either ends in a Done (no further
            // tool calls in flight) or in a tool dispatch — in which case
            // we append the tool result to the prompt and re-stream up to
            // MAX_STREAM_ITERATIONS times.
            String prompt = renderPrompt(systemPrompt, session.getMessages(), List.of());
            int iter = 0;
            boolean continueLoop = true;
            while (continueLoop) {
                continueLoop = false;
                List<ToolCall> iterToolCalls = new ArrayList<>();

                geminiClient.streamGenerate(prompt, null, null, tools, chunk -> {
                    if (finished.get()) return;
                    handleChunk(chunk, emitter, accumulated, iterToolCalls, cancel, userId);
                }, cancel);

                if (finished.get() || cancel.isCancelled()) {
                    return;
                }

                // Tool dispatch happens AFTER the inner stream completes
                // — Gemini's streaming function-call protocol delivers the
                // FunctionCall chunk and then a Done; we dispatch here so
                // the SSE event order on the client is:
                //   tool_call -> tool_result -> (next stream's tokens)
                for (ToolCall tc : iterToolCalls) {
                    Object result;
                    final String dispatchedTool = tc.getName();
                    ToolDispatchContext dispatchCtx = new ToolDispatchContext(
                            userId, routeContext,
                            ev -> { /* phase-5 wires this to SSE tool_progress */ });
                    try {
                        result = toolHandler.dispatch(dispatchedTool, tc.getArgs(), dispatchCtx);
                    } catch (Exception e) {
                        // Tool failures get a generic user-facing message
                        // + correlation id (same policy as the outer
                        // error path). The raw exception goes to logs only.
                        String corrId = UUID.randomUUID().toString();
                        log.warn("Chat tool '{}' failed corrId={}", tc.getName(), corrId, e);
                        Map<String, Object> err = new LinkedHashMap<>();
                        err.put("error", "Tool indisponibil temporar.");
                        err.put("correlationId", corrId);
                        result = err;
                    }
                    tc.setResult(result);
                    capturedToolCalls.add(tc);
                    Map<String, Object> toolResultPayload = new LinkedHashMap<>();
                    toolResultPayload.put("name", tc.getName());
                    toolResultPayload.put("result", result);
                    sendEvent(emitter, "tool_result", toolResultPayload, cancel);
                }

                iter++;
                // If we got tool calls AND we still have budget, re-prompt
                // Gemini with the tool result in context so it can produce
                // a final natural-language reply.
                if (!iterToolCalls.isEmpty() && iter < MAX_STREAM_ITERATIONS) {
                    prompt = renderPrompt(systemPrompt, session.getMessages(), capturedToolCalls);
                    continueLoop = true;
                }
            }

            if (finished.get() || cancel.isCancelled()) {
                return;
            }

            // Persist the assistant message (text + any tool calls).
            ChatMessage assistantMsg = new ChatMessage();
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(accumulated.toString());
            assistantMsg.setToolCalls(capturedToolCalls.isEmpty() ? null : capturedToolCalls);
            assistantMsg.setTs(Instant.now());

            ChatSession savedSession = chatService.appendMessage(session, assistantMsg);

            // Auto-title from first user message if still default. We look
            // for the FIRST user message in the session — that's what the
            // user originally typed to start the thread.
            //
            // Use the focused updateTitle helper rather than a full save:
            // a save() here would race the next user-turn's appendMessage
            // and either lose a message or trip the @Version check.
            if (ChatService.DEFAULT_TITLE.equals(savedSession.getTitle())) {
                String firstUser = findFirstUserContent(savedSession);
                if (firstUser != null && !firstUser.isBlank()) {
                    String newTitle = firstUser.length() > TITLE_FROM_USER_MAX_LEN
                            ? firstUser.substring(0, TITLE_FROM_USER_MAX_LEN) + "..."
                            : firstUser;
                    savedSession.setTitle(newTitle);
                    chatService.updateTitle(savedSession.getId(), userId, newTitle);
                }
            }

            Map<String, Object> donePayload = new LinkedHashMap<>();
            donePayload.put("sessionId", savedSession.getId());
            donePayload.put("title", savedSession.getTitle());
            if (!capturedToolCalls.isEmpty()) {
                donePayload.put("toolCalls", capturedToolCalls);
            }
            sendEvent(emitter, "done", donePayload, cancel);
            emitter.complete();
        } catch (Exception e) {
            if (cancel.isCancelled() || finished.get()) {
                log.debug("Chat stream aborted after cancel session={}", session.getId());
                return;
            }
            // Generic user-facing error + correlation id so the FE bug
            // report stays joinable to our server logs. Never surface
            // the raw exception text — upstream messages can leak
            // sensitive detail (api key fragments, internal hostnames).
            String corrId = UUID.randomUUID().toString();
            log.warn("Chat stream failed session={} corrId={}", session.getId(), corrId, e);
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("message", "Serviciul AI este temporar indisponibil.");
                payload.put("correlationId", corrId);
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException ignored) {
                // best effort — client may be gone
            }
            emitter.complete();
        } finally {
            shutdown.run();
        }
    }

    private void handleChunk(GeminiStreamChunk chunk,
                             SseEmitter emitter,
                             StringBuilder accumulated,
                             List<ToolCall> iterToolCalls,
                             GeminiClient.StreamCancelHandle cancel,
                             String userId) {
        try {
            if (chunk instanceof GeminiStreamChunk.TextDelta delta) {
                accumulated.append(delta.text());
                // LinkedHashMap rather than Map.of: keeps insertion order
                // stable for Jackson serialization and tolerates null
                // values, which Gemini occasionally produces for fc.args().
                Map<String, Object> tokenPayload = new LinkedHashMap<>();
                tokenPayload.put("text", delta.text());
                emitter.send(SseEmitter.event()
                        .name("token")
                        .data(tokenPayload, MediaType.APPLICATION_JSON));
            } else if (chunk instanceof GeminiStreamChunk.FunctionCall fc) {
                ToolCall tc = new ToolCall(fc.name(), fc.args(), null);
                iterToolCalls.add(tc);
                Map<String, Object> toolCallPayload = new LinkedHashMap<>();
                toolCallPayload.put("name", fc.name());
                toolCallPayload.put("args", fc.args());
                emitter.send(SseEmitter.event()
                        .name("tool_call")
                        .data(toolCallPayload, MediaType.APPLICATION_JSON));
            } else if (chunk instanceof GeminiStreamChunk.Done done) {
                log.debug("Chat stream chunk done user={} finishReason={}", userId, done.finishReason());
            }
        } catch (IOException ioe) {
            log.debug("Chat SSE send failed (client gone?): {}", ioe.getClass().getSimpleName());
            cancel.cancel();
        }
    }

    /**
     * Best-effort SSE send that flips the cancel handle on IO error.
     * Used after-the-fact (post-tool-dispatch) — the inner chunk handler
     * has its own inline error path.
     */
    private void sendEvent(SseEmitter emitter,
                           String name,
                           Object payload,
                           GeminiClient.StreamCancelHandle cancel) {
        try {
            emitter.send(SseEmitter.event().name(name).data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException ioe) {
            log.debug("Chat SSE post-event send failed: {}", ioe.getClass().getSimpleName());
            cancel.cancel();
        }
    }

    /**
     * Serializes the system prompt + the current message window + (if
     * provided) any accumulated tool results into a single text prompt.
     *
     * <p>Why a single text blob instead of multi-turn Gemini contents:
     * the existing {@link GeminiClient#streamGenerate} signature accepts
     * one prompt string. A multi-turn variant can be added later — for
     * now, a structured plain-text rendering plays well enough with the
     * model and keeps the contract narrow.
     */
    String renderPrompt(String systemPrompt, List<ChatMessage> messages, List<ToolCall> toolResults) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(systemPrompt).append("\n\n");
        sb.append("=== Conversatie ===\n");
        if (messages != null) {
            for (ChatMessage m : messages) {
                sb.append('[').append(m.getRole()).append("]\n");
                if (m.getContent() != null && !m.getContent().isBlank()) {
                    sb.append(m.getContent()).append('\n');
                }
                if (m.getToolCalls() != null) {
                    for (ToolCall tc : m.getToolCalls()) {
                        sb.append("[tool_call] ").append(tc.getName())
                                .append(' ').append(safeJson(tc.getArgs())).append('\n');
                        if (tc.getResult() != null) {
                            sb.append("[tool_result] ").append(safeJson(tc.getResult())).append('\n');
                        }
                    }
                }
            }
        }
        if (toolResults != null && !toolResults.isEmpty()) {
            sb.append("\n=== Rezultate tool ===\n");
            for (ToolCall tc : toolResults) {
                sb.append(tc.getName()).append(": ").append(safeJson(tc.getResult())).append('\n');
            }
            sb.append("\nFormuleaza un raspuns scurt in romana, mentionand ca draft-ul a fost generat.");
        }
        return sb.toString();
    }

    private String safeJson(Object o) {
        if (o == null) return "null";
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    /**
     * Declares the two tools available to the chatbot. These match the
     * dispatcher cases in {@link ChatToolHandler}. Kept in this service
     * (not the handler) because the declarations are coupled to the
     * Gemini request shape — the handler only cares about the args at
     * dispatch time.
     */
    static List<Map<String, Object>> buildToolDeclarations() {
        Map<String, Object> createQuizDraft = Map.of(
                "name", "createQuizDraft",
                "description", "Genereaza un draft de quiz pe un topic. Foloseste cand utilizatorul cere explicit un quiz.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "topic", Map.of("type", "string"),
                                "questionCount", Map.of("type", "integer", "minimum", 3, "maximum", 20),
                                "difficulty", Map.of("type", "string", "enum", List.of("easy", "medium", "hard")),
                                "includeFreeText", Map.of("type", "boolean")
                        ),
                        "required", List.of("topic", "questionCount")
                )
        );
        Map<String, Object> createCourseDraft = Map.of(
                "name", "createCourseDraft",
                "description", "Genereaza schema unui curs (titlu, descriere, module cu titluri de lectii). Foloseste cand utilizatorul cere explicit un curs nou.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "subject", Map.of("type", "string"),
                                "audience", Map.of("type", "string"),
                                "moduleCount", Map.of("type", "integer", "minimum", 3, "maximum", 10)
                        ),
                        "required", List.of("subject", "audience", "moduleCount")
                )
        );

        // ── buildFullCourse — side-effecting, persists draft course ──
        Map<String, Object> buildFullCourseProps = new LinkedHashMap<>();
        buildFullCourseProps.put("subject", Map.of("type", "string"));
        buildFullCourseProps.put("audience", Map.of("type", "string"));
        buildFullCourseProps.put("moduleCount",
                Map.of("type", "integer", "minimum", 2, "maximum", 8));
        buildFullCourseProps.put("lecturesPerModule",
                Map.of("type", "integer", "minimum", 1, "maximum", 6));
        buildFullCourseProps.put("questionsPerQuiz",
                Map.of("type", "integer", "minimum", 3, "maximum", 12));
        buildFullCourseProps.put("includeQuizzes", Map.of("type", "boolean"));
        buildFullCourseProps.put("language", Map.of("type", "string"));
        Map<String, Object> buildFullCourse = Map.of(
                "name", "buildFullCourse",
                "description",
                "Construieste si salveaza un curs complet (module + lectii + quiz-uri) ca draft. "
                        + "Foloseste cand profesorul cere explicit crearea integrala a unui curs.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", buildFullCourseProps,
                        "required", List.of("subject", "audience", "moduleCount")
                )
        );

        // ── Modify tools — all require routeContext.courseId server-side. ──
        String modifyHint = " Foloseste ID-urile exact din STAREA CURSULUI ACTIV "
                + "din prompt-ul de sistem. Nu inventa ID-uri.";

        Map<String, Object> addModule = Map.of(
                "name", "addModule",
                "description", "Adauga un modul nou la cursul activ." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "title", Map.of("type", "string"),
                                "description", Map.of("type", "string")
                        ),
                        "required", List.of("title")
                )
        );

        Map<String, Object> updateModule = Map.of(
                "name", "updateModule",
                "description", "Actualizeaza titlul si/sau descrierea unui modul existent." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "moduleId", Map.of("type", "string"),
                                "title", Map.of("type", "string"),
                                "description", Map.of("type", "string")
                        ),
                        "required", List.of("moduleId")
                )
        );

        Map<String, Object> deleteModule = Map.of(
                "name", "deleteModule",
                "description", "Sterge un modul (cu toate lectiile si quiz-ul lui)." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "moduleId", Map.of("type", "string")
                        ),
                        "required", List.of("moduleId")
                )
        );

        Map<String, Object> reorderModules = Map.of(
                "name", "reorderModules",
                "description", "Schimba ordinea modulelor in curs." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "orderedIds", Map.of(
                                        "type", "array",
                                        "items", Map.of("type", "string"),
                                        "minItems", 1
                                )
                        ),
                        "required", List.of("orderedIds")
                )
        );

        Map<String, Object> addLectureProps = new LinkedHashMap<>();
        addLectureProps.put("moduleId", Map.of("type", "string"));
        addLectureProps.put("title", Map.of("type", "string"));
        addLectureProps.put("content", Map.of("type", "string"));
        addLectureProps.put("durationSecs", Map.of("type", "integer", "minimum", 0));
        Map<String, Object> addLecture = Map.of(
                "name", "addLecture",
                "description", "Adauga o lectie noua intr-un modul." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", addLectureProps,
                        "required", List.of("moduleId", "title")
                )
        );

        Map<String, Object> updateLectureProps = new LinkedHashMap<>();
        updateLectureProps.put("moduleId", Map.of("type", "string"));
        updateLectureProps.put("lectureId", Map.of("type", "string"));
        updateLectureProps.put("title", Map.of("type", "string"));
        updateLectureProps.put("content", Map.of("type", "string"));
        updateLectureProps.put("durationSecs", Map.of("type", "integer", "minimum", 0));
        Map<String, Object> updateLecture = Map.of(
                "name", "updateLecture",
                "description", "Actualizeaza titlul si/sau continutul unei lectii." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", updateLectureProps,
                        "required", List.of("moduleId", "lectureId")
                )
        );

        Map<String, Object> deleteLecture = Map.of(
                "name", "deleteLecture",
                "description", "Sterge o lectie dintr-un modul." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "moduleId", Map.of("type", "string"),
                                "lectureId", Map.of("type", "string")
                        ),
                        "required", List.of("moduleId", "lectureId")
                )
        );

        Map<String, Object> reorderLectures = Map.of(
                "name", "reorderLectures",
                "description", "Schimba ordinea lectiilor dintr-un modul." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "moduleId", Map.of("type", "string"),
                                "orderedIds", Map.of(
                                        "type", "array",
                                        "items", Map.of("type", "string"),
                                        "minItems", 1
                                )
                        ),
                        "required", List.of("moduleId", "orderedIds")
                )
        );

        // Quiz question shape reused for add/update.
        Map<String, Object> quizQuestionProps = new LinkedHashMap<>();
        quizQuestionProps.put("text", Map.of("type", "string"));
        quizQuestionProps.put("type", Map.of(
                "type", "string",
                "enum", List.of("multiple_choice")
        ));
        quizQuestionProps.put("options", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "minItems", 2,
                "maxItems", 4
        ));
        quizQuestionProps.put("correctIdx",
                Map.of("type", "integer", "minimum", 0, "maximum", 3));
        quizQuestionProps.put("explanation", Map.of("type", "string"));
        Map<String, Object> quizQuestionSchema = Map.of(
                "type", "object",
                "properties", quizQuestionProps,
                "required", List.of("text", "type", "options", "correctIdx")
        );

        Map<String, Object> addModuleQuizProps = new LinkedHashMap<>();
        addModuleQuizProps.put("moduleId", Map.of("type", "string"));
        addModuleQuizProps.put("title", Map.of("type", "string"));
        addModuleQuizProps.put("passingScore",
                Map.of("type", "integer", "minimum", 0, "maximum", 100));
        addModuleQuizProps.put("timeLimit", Map.of("type", "integer", "minimum", 0));
        addModuleQuizProps.put("questions", Map.of(
                "type", "array",
                "items", quizQuestionSchema,
                "minItems", 1
        ));
        Map<String, Object> addModuleQuiz = Map.of(
                "name", "addModuleQuiz",
                "description",
                "Creeaza (sau inlocuieste) quiz-ul atasat unui modul." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", addModuleQuizProps,
                        "required", List.of("moduleId", "title", "questions")
                )
        );

        Map<String, Object> updateModuleQuiz = Map.of(
                "name", "updateModuleQuiz",
                "description",
                "Actualizeaza quiz-ul unui modul (titlu sau intrebari)." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", addModuleQuizProps,
                        "required", List.of("moduleId")
                )
        );

        Map<String, Object> deleteModuleQuiz = Map.of(
                "name", "deleteModuleQuiz",
                "description", "Sterge quiz-ul atasat unui modul." + modifyHint,
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "moduleId", Map.of("type", "string")
                        ),
                        "required", List.of("moduleId")
                )
        );

        return List.of(
                createQuizDraft, createCourseDraft, buildFullCourse,
                addModule, updateModule, deleteModule, reorderModules,
                addLecture, updateLecture, deleteLecture, reorderLectures,
                addModuleQuiz, updateModuleQuiz, deleteModuleQuiz
        );
    }

    private static String findFirstUserContent(ChatSession session) {
        if (session.getMessages() == null) return null;
        for (ChatMessage m : session.getMessages()) {
            if ("user".equals(m.getRole()) && m.getContent() != null && !m.getContent().isBlank()) {
                return m.getContent();
            }
        }
        return null;
    }

}
