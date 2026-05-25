package ro.fiismart.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ro.fiismart.ai.client.GeminiClient;
import ro.fiismart.ai.client.GeminiStreamChunk;
import ro.fiismart.ai.dto.response.PdfAiGenerateResponse;
import ro.fiismart.ai.service.PdfAiPrompts;
import ro.fiismart.ai.service.PdfAiService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class PdfAiController {

    /** ~10 min: comfortably covers the longest Gemini stream + 15s pings. */
    private static final long SSE_TIMEOUT_MS = 10L * 60L * 1000L;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 15L;

    private final PdfAiService service;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    /**
     * Shared scheduler for SSE heartbeats. Daemon threads so JVM shutdown
     * is not blocked by idle pings. One pool covers every concurrent
     * stream — each emitter takes a single recurring task.
     *
     * <p>Pool size scales with the available CPUs (with a floor of 4) so
     * that the third+ concurrent SSE stream can still get its heartbeat
     * ticks — otherwise idle proxies tear the connection down.</p>
     */
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newScheduledThreadPool(
                    Math.max(4, Runtime.getRuntime().availableProcessors()),
                    r -> {
                        Thread t = new Thread(r, "ai-sse-heartbeat");
                        t.setDaemon(true);
                        return t;
                    });

    /**
     * Shut the heartbeat pool down on application shutdown so we don't
     * leak threads on hot redeploys. Daemons would die with the JVM, but
     * Spring devtools / test contexts reuse the JVM — explicit shutdown
     * keeps reload behaviour clean.
     */
    @PreDestroy
    void shutdownHeartbeat() {
        heartbeatScheduler.shutdownNow();
    }

    @PostMapping(value = "/pdf/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROFESSOR')")
    public PdfAiGenerateResponse generate(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "5") int questionCount,
            @RequestParam(defaultValue = "ro") String language,
            @AuthenticationPrincipal String userId) throws IOException {
        log.info("AI PDF generate: user={} file={} size={} qCount={} lang={}",
                userId, file.getOriginalFilename(), file.getSize(), questionCount, language);
        return service.generate(file, questionCount, language);
    }

    /**
     * Streaming counterpart: accepts the same multipart form, streams
     * Gemini text deltas to the client as SSE {@code event: token}, and
     * emits a terminal {@code event: done} with the parsed
     * {summary, quiz} structure. Errors surface as {@code event: error}.
     *
     * <p>Heartbeats (event: ping) every 15s defeat idle-proxy timeouts.
     * Client disconnects flip a cancel handle that aborts the upstream
     * Gemini call at the next chunk boundary.
     */
    @PostMapping(
            value = "/pdf/generate/stream",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('PROFESSOR')")
    public SseEmitter generateStream(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "5") int questionCount,
            @RequestParam(defaultValue = "ro") String language,
            @AuthenticationPrincipal String userId) {

        log.info("AI PDF stream: user={} file={} size={} qCount={} lang={}",
                userId, file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : -1, questionCount, language);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // Validate eagerly. If invalid, we still return an emitter so the
        // FE consistently consumes errors via the SSE channel (no mixed
        // 400-then-SSE story). Worker thread handles the actual work.
        byte[] pdfBytes;
        String normalizedLang;
        try {
            service.validateInputs(file, questionCount, language);
            pdfBytes = file.getBytes();
            normalizedLang = service.normalizeLanguage(language);
        } catch (IllegalArgumentException iae) {
            log.warn("AI PDF stream rejected input: {}", iae.getMessage());
            sendErrorAndComplete(emitter, iae.getMessage());
            return emitter;
        } catch (IOException ioe) {
            log.warn("AI PDF stream could not read upload: {}", ioe.getClass().getSimpleName());
            sendErrorAndComplete(emitter, "Cannot read uploaded PDF");
            return emitter;
        }

        String prompt = PdfAiPrompts.buildPrompt(questionCount, normalizedLang);
        Map<String, Object> schema = PdfAiPrompts.RESPONSE_SCHEMA;

        // Per-request single-thread executor: the Gemini stream call is
        // long-running and blocking on the HTTP body; we need to keep it
        // off the Tomcat worker. Shut down on completion.
        ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ai-sse-stream-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });

        GeminiClient.StreamCancelHandle cancel = new GeminiClient.StreamCancelHandle();
        AtomicBoolean finished = new AtomicBoolean(false);

        // Schedule heartbeat pings every 15s. Cancel as soon as the
        // stream finishes (any path: success / error / disconnect).
        //
        // If `send` throws, the emitter is already dead — cancel our own
        // schedule rather than spin re-failing every interval and
        // wasting a scheduler slot until shutdown runs.
        AtomicReference<ScheduledFuture<?>> heartbeatRef = new AtomicReference<>();
        ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (finished.get()) return;
            try {
                emitter.send(SseEmitter.event().name("ping").data("{}"));
            } catch (Exception e) {
                log.debug("Heartbeat send failed (emitter closed?): {}", e.getClass().getSimpleName());
                ScheduledFuture<?> self = heartbeatRef.get();
                if (self != null) self.cancel(false);
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        heartbeatRef.set(heartbeat);

        Runnable shutdown = () -> {
            if (finished.compareAndSet(false, true)) {
                cancel.cancel();
                heartbeat.cancel(false);
                worker.shutdownNow();
            }
        };

        emitter.onCompletion(shutdown);
        emitter.onTimeout(() -> {
            log.info("AI PDF stream emitter timed out user={}", userId);
            shutdown.run();
        });
        emitter.onError(t -> {
            log.warn("AI PDF stream emitter error user={}: {}", userId, t.getClass().getSimpleName());
            shutdown.run();
        });

        worker.submit(() -> runStream(emitter, prompt, pdfBytes, schema, cancel, finished, shutdown, userId));

        return emitter;
    }

    private void runStream(SseEmitter emitter,
                           String prompt,
                           byte[] pdfBytes,
                           Map<String, Object> schema,
                           GeminiClient.StreamCancelHandle cancel,
                           AtomicBoolean finished,
                           Runnable shutdown,
                           String userId) {
        StringBuilder accumulated = new StringBuilder();
        try {
            geminiClient.streamGenerate(prompt, pdfBytes, schema, chunk -> {
                if (finished.get()) return;
                try {
                    if (chunk instanceof GeminiStreamChunk.TextDelta delta) {
                        accumulated.append(delta.text());
                        // LinkedHashMap: tolerates null values and keeps
                        // insertion order stable across runs (Jackson
                        // walks the entry-set in order).
                        Map<String, Object> tokenPayload = new LinkedHashMap<>();
                        tokenPayload.put("text", delta.text());
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(tokenPayload, MediaType.APPLICATION_JSON));
                    } else if (chunk instanceof GeminiStreamChunk.FunctionCall fc) {
                        // Not expected on the PDF path, but forward defensively
                        Map<String, Object> toolCallPayload = new LinkedHashMap<>();
                        toolCallPayload.put("name", fc.name());
                        toolCallPayload.put("args", fc.args());
                        emitter.send(SseEmitter.event()
                                .name("tool_call")
                                .data(toolCallPayload, MediaType.APPLICATION_JSON));
                    } else if (chunk instanceof GeminiStreamChunk.Done done) {
                        log.debug("AI PDF stream done user={} finishReason={}", userId, done.finishReason());
                    }
                } catch (IOException ioe) {
                    // Client likely disconnected — flip cancel so the
                    // streamGenerate loop bails on next chunk.
                    log.debug("SSE send failed (client gone?): {}", ioe.getClass().getSimpleName());
                    cancel.cancel();
                }
            }, cancel);

            if (finished.get() || cancel.isCancelled()) {
                return;
            }

            // Parse the accumulated JSON into the final structured response.
            PdfAiGenerateResponse response = service.parseAndValidateGeneratedJson(accumulated.toString());
            String payload = objectMapper.writeValueAsString(response);
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(payload, MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (Exception e) {
            if (cancel.isCancelled() || finished.get()) {
                log.debug("AI PDF stream aborted after cancel user={}", userId);
                return;
            }
            // User-facing payload deliberately omits the exception text:
            // upstream messages can include stack-trace-ish hints or API
            // keys in pathological cases. Correlate via the corrId in
            // the server log instead.
            String corrId = UUID.randomUUID().toString();
            log.warn("AI PDF stream failed user={} corrId={}", userId, corrId, e);
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("message", "Serviciul AI este temporar indisponibil.");
                payload.put("correlationId", corrId);
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException ignored) {
                // best effort
            }
            emitter.complete();
        } finally {
            shutdown.run();
        }
    }

    private static void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("message", message);
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException ignored) {
            // emitter not yet open / client gone — nothing actionable
        }
        emitter.complete();
    }

}
