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
 * <p>Tool iterations are hard-capped at {@link #MAX_TOOL_ITERATIONS} per
 * user turn to defend against a model that keeps calling tools in a loop.
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

    /** Soft cap on tool calls per user turn to avoid runaway loops. */
    static final int MAX_TOOL_ITERATIONS = 2;

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
            // MAX_TOOL_ITERATIONS times.
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
                    try {
                        result = toolHandler.dispatch(tc.getName(), tc.getArgs(), userId);
                    } catch (Exception e) {
                        log.warn("Chat tool '{}' failed: {}", tc.getName(), e.getClass().getSimpleName());
                        result = Map.of("error", safeMessage(e));
                    }
                    tc.setResult(result);
                    capturedToolCalls.add(tc);
                    sendEvent(emitter, "tool_result",
                            Map.of("name", tc.getName(), "result", result),
                            cancel);
                }

                iter++;
                // If we got tool calls AND we still have budget, re-prompt
                // Gemini with the tool result in context so it can produce
                // a final natural-language reply.
                if (!iterToolCalls.isEmpty() && iter < MAX_TOOL_ITERATIONS) {
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
            String newTitle = null;
            if (ChatService.DEFAULT_TITLE.equals(savedSession.getTitle())) {
                String firstUser = findFirstUserContent(savedSession);
                if (firstUser != null && !firstUser.isBlank()) {
                    newTitle = firstUser.length() > TITLE_FROM_USER_MAX_LEN
                            ? firstUser.substring(0, TITLE_FROM_USER_MAX_LEN) + "..."
                            : firstUser;
                    savedSession.setTitle(newTitle);
                    chatService.save(savedSession);
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
            log.warn("Chat stream failed session={} err={}", session.getId(), e.getClass().getSimpleName());
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("message", safeMessage(e)), MediaType.APPLICATION_JSON));
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
                emitter.send(SseEmitter.event()
                        .name("token")
                        .data(Map.of("text", delta.text()), MediaType.APPLICATION_JSON));
            } else if (chunk instanceof GeminiStreamChunk.FunctionCall fc) {
                ToolCall tc = new ToolCall(fc.name(), fc.args(), null);
                iterToolCalls.add(tc);
                emitter.send(SseEmitter.event()
                        .name("tool_call")
                        .data(Map.of("name", fc.name(), "args", fc.args()),
                                MediaType.APPLICATION_JSON));
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
        return List.of(createQuizDraft, createCourseDraft);
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

    private static String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return msg == null || msg.isBlank() ? t.getClass().getSimpleName() : msg;
    }
}
