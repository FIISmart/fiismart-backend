package ro.fiismart.ai.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import ro.fiismart.ai.config.GeminiProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private static final Set<String> BAD_FINISH_REASONS = Set.of("SAFETY", "RECITATION", "OTHER");
    /** Statuses that are worth retrying — Google's lite models routinely return 503/429 under load. */
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 500, 502, 503, 504);
    private static final int MAX_ATTEMPTS = 3;

    private final GeminiProperties properties;
    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateJson(String prompt, byte[] pdfBytes, Map<String, Object> responseSchema) {
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("inline_data", Map.of(
                                        "mime_type", "application/pdf",
                                        "data", base64Pdf
                                )),
                                Map.of("text", prompt)
                        )
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                )
        );

        log.info("Gemini call model={} promptLen={} pdfBytes={}",
                properties.getModel(), prompt.length(), pdfBytes.length);

        return invokeGenerateContent(body);
    }

    /**
     * Text-only JSON-mode call — same behaviour as {@link #generateJson(String, byte[], Map)}
     * but without an inline PDF part. Used by services that grade or summarize
     * plain text (e.g. free-text quiz grading).
     *
     * <p>This is a thin overload on top of the existing retry/parse pipeline:
     * adding it here keeps the request/response handling in one place rather
     * than duplicating it across services.</p>
     */
    public String generateJson(String prompt, Map<String, Object> responseSchema) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                )
        );

        log.info("Gemini call (text-only) model={} promptLen={}",
                properties.getModel(), prompt.length());

        return invokeGenerateContent(body);
    }

    /**
     * Shared post-call pipeline used by both PDF-bearing and text-only
     * {@code generateJson} variants: drives {@link #callWithRetry} and
     * extracts the first candidate's text part (or throws a sanitized
     * {@link GeminiException}).
     */
    private String invokeGenerateContent(Map<String, Object> body) {
        String rawResponse = callWithRetry(body);

        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("Gemini returned empty response body");
            throw new GeminiException("Gemini returned empty response");
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                log.warn("Gemini response missing candidates");
                throw new GeminiException("Gemini response missing candidates");
            }

            JsonNode firstCandidate = candidates.get(0);
            String finishReason = firstCandidate.path("finishReason").asText("");
            if (BAD_FINISH_REASONS.contains(finishReason)) {
                log.warn("Gemini finishReason={}", finishReason);
                throw new GeminiException("Gemini blocked content with finishReason=" + finishReason);
            }

            JsonNode parts = firstCandidate.path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                log.warn("Gemini candidate has no parts");
                throw new GeminiException("Gemini candidate has no parts");
            }

            String text = parts.get(0).path("text").asText("");
            if (text.isBlank()) {
                log.warn("Gemini candidate text is empty");
                throw new GeminiException("Gemini candidate text is empty");
            }
            return text;
        } catch (GeminiException ge) {
            throw ge;
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response: {}", e.getClass().getSimpleName());
            throw new GeminiException("Failed to parse Gemini response", e);
        }
    }

    /**
     * Streaming counterpart of {@link #generateJson}. Opens
     * {@code :streamGenerateContent?alt=sse} and forwards each parsed chunk
     * to {@code onChunk} on the caller's thread (this method blocks until
     * the upstream stream completes / errors / is cancelled).
     *
     * <p>Retry on 429/5xx applies ONLY to the initial connection. Once we
     * start consuming the response body, any I/O failure is surfaced via
     * {@code onError} — partial streams are not retried (Gemini does not
     * support resume).
     *
     * @param prompt         the user prompt
     * @param pdfBytes       optional inline PDF (same shape as
     *                       {@link #generateJson}); null when not used
     * @param responseSchema optional JSON-mode schema; null for free text /
     *                       tool-using calls
     * @param tools          optional Gemini tool declarations (function
     *                       calling); null when no tools
     * @param onChunk        sink for {@link GeminiStreamChunk} events
     * @param cancelHandle   handle the caller can interrogate / use to
     *                       cancel the in-flight request (e.g. on SSE
     *                       client disconnect). May be null.
     */
    public void streamGenerate(
            String prompt,
            byte[] pdfBytes,
            Map<String, Object> responseSchema,
            List<Map<String, Object>> tools,
            Consumer<GeminiStreamChunk> onChunk,
            StreamCancelHandle cancelHandle) {

        Map<String, Object> body = buildStreamBody(prompt, pdfBytes, responseSchema, tools);

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds() / 4)))
                .build();

        String url = properties.getBaseUrl()
                + "/v1beta/models/" + properties.getModel()
                + ":streamGenerateContent?alt=sse";

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new GeminiException("Failed to serialize Gemini stream request", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("x-goog-api-key", properties.getKey())
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = openStreamWithRetry(http, request, cancelHandle);

        try (InputStream raw = response.body();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(raw, StandardCharsets.UTF_8))) {

            consumeSse(reader, onChunk, cancelHandle);
        } catch (IOException e) {
            if (cancelHandle != null && cancelHandle.isCancelled()) {
                log.debug("Gemini stream cancelled by caller");
                return;
            }
            throw new GeminiException("Gemini stream I/O error", e);
        }
    }

    /**
     * Convenience overload — no tools, no schema.
     */
    public void streamGenerate(
            String prompt,
            byte[] pdfBytes,
            Map<String, Object> responseSchema,
            Consumer<GeminiStreamChunk> onChunk,
            StreamCancelHandle cancelHandle) {
        streamGenerate(prompt, pdfBytes, responseSchema, null, onChunk, cancelHandle);
    }

    private Map<String, Object> buildStreamBody(
            String prompt,
            byte[] pdfBytes,
            Map<String, Object> responseSchema,
            List<Map<String, Object>> tools) {

        List<Map<String, Object>> parts = new ArrayList<>(2);
        if (pdfBytes != null && pdfBytes.length > 0) {
            parts.add(Map.of("inline_data", Map.of(
                    "mime_type", "application/pdf",
                    "data", Base64.getEncoder().encodeToString(pdfBytes)
            )));
        }
        parts.add(Map.of("text", prompt));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("parts", parts)));

        if (responseSchema != null) {
            body.put("generationConfig", Map.of(
                    "responseMimeType", "application/json",
                    "responseSchema", responseSchema
            ));
        }
        if (tools != null && !tools.isEmpty()) {
            // Gemini expects: tools: [{ functionDeclarations: [...] }]
            body.put("tools", List.of(Map.of("functionDeclarations", tools)));
        }
        return body;
    }

    /**
     * Opens the SSE response, retrying only the initial HTTP exchange on
     * 429/5xx (same backoff policy as {@link #callWithRetry}). Once we have
     * a 2xx with a streaming body, we hand it back; any later body-side
     * error is treated as fatal by the caller.
     */
    private HttpResponse<InputStream> openStreamWithRetry(
            HttpClient http, HttpRequest request, StreamCancelHandle cancelHandle) {

        int lastStatus = -1;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (cancelHandle != null && cancelHandle.isCancelled()) {
                throw new GeminiException("Gemini stream cancelled before connect");
            }
            try {
                HttpResponse<InputStream> resp = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = resp.statusCode();
                if (status >= 200 && status < 300) {
                    log.info("Gemini stream open model={} attempt={}/{}",
                            properties.getModel(), attempt, MAX_ATTEMPTS);
                    return resp;
                }
                // Non-2xx: drain body for the log line, then decide retry/abort.
                String errorBody;
                try (InputStream b = resp.body()) {
                    errorBody = new String(b.readAllBytes(), StandardCharsets.UTF_8);
                }
                lastStatus = status;
                if (RETRYABLE_STATUSES.contains(status) && attempt < MAX_ATTEMPTS) {
                    long backoffMs = 500L * (long) Math.pow(2, attempt - 1);
                    log.warn("Gemini stream status={} attempt={}/{} — retrying in {}ms",
                            status, attempt, MAX_ATTEMPTS, backoffMs);
                    sleepQuietly(backoffMs);
                    continue;
                }
                log.warn("Gemini stream failed status={} body={}", status,
                        errorBody.length() > 800 ? errorBody.substring(0, 800) + "..." : errorBody);
                throw new GeminiException("Gemini upstream HTTP error: " + status);
            } catch (GeminiException ge) {
                throw ge;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new GeminiException("Gemini stream interrupted", ie);
            } catch (IOException ioe) {
                if (attempt < MAX_ATTEMPTS) {
                    long backoffMs = 500L * (long) Math.pow(2, attempt - 1);
                    log.warn("Gemini stream connect I/O attempt={}/{} — retrying in {}ms: {}",
                            attempt, MAX_ATTEMPTS, backoffMs, ioe.getClass().getSimpleName());
                    sleepQuietly(backoffMs);
                    continue;
                }
                throw new GeminiException("Gemini stream connect failed", ioe);
            }
        }
        throw new GeminiException("Gemini upstream busy after "
                + MAX_ATTEMPTS + " attempts (lastStatus=" + lastStatus + ")");
    }

    /**
     * Parses the SSE stream: each event is {@code data: <json>\n\n}.
     * Extracts text deltas, function calls, and finishReason from each
     * Gemini chunk and pushes them via {@code onChunk}.
     *
     * <p>Visibility {@code package-private} so unit tests can exercise the
     * parser directly with a synthetic reader (no socket needed).
     */
    void consumeSse(BufferedReader reader,
                    Consumer<GeminiStreamChunk> onChunk,
                    StreamCancelHandle cancelHandle) throws IOException {

        AtomicReference<String> finalFinishReason = new AtomicReference<>(null);
        StringBuilder dataBuf = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (cancelHandle != null && cancelHandle.isCancelled()) {
                log.debug("Gemini SSE consume cancelled");
                return;
            }
            if (line.isEmpty()) {
                // event terminator — flush whatever is in dataBuf
                if (dataBuf.length() > 0) {
                    handleSseData(dataBuf.toString(), onChunk, finalFinishReason);
                    dataBuf.setLength(0);
                }
                continue;
            }
            if (line.startsWith(":")) {
                // SSE comment — ignore
                continue;
            }
            if (line.startsWith("data:")) {
                String payload = line.substring(5);
                if (payload.startsWith(" ")) {
                    payload = payload.substring(1);
                }
                dataBuf.append(payload);
            }
            // Other SSE fields (event:, id:, retry:) aren't used by Gemini.
        }
        // Stream closed — flush trailing data (no blank line terminator).
        if (dataBuf.length() > 0) {
            handleSseData(dataBuf.toString(), onChunk, finalFinishReason);
        }
        String reason = finalFinishReason.get();
        onChunk.accept(new GeminiStreamChunk.Done(reason == null ? "STOP" : reason));
    }

    private void handleSseData(String data,
                               Consumer<GeminiStreamChunk> onChunk,
                               AtomicReference<String> finalFinishReason) {
        if (data == null || data.isEmpty() || "[DONE]".equals(data.trim())) {
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(data);
        } catch (Exception e) {
            log.warn("Gemini SSE: skipping unparsable chunk ({} chars)", data.length());
            return;
        }

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return;
        }
        JsonNode candidate = candidates.get(0);

        JsonNode parts = candidate.path("content").path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                JsonNode textNode = part.get("text");
                if (textNode != null && textNode.isTextual()) {
                    String t = textNode.asText();
                    if (!t.isEmpty()) {
                        onChunk.accept(new GeminiStreamChunk.TextDelta(t));
                    }
                    continue;
                }
                JsonNode fc = part.get("functionCall");
                if (fc != null && fc.isObject()) {
                    String name = fc.path("name").asText("");
                    Map<String, Object> args = new HashMap<>();
                    JsonNode argsNode = fc.path("args");
                    if (argsNode.isObject()) {
                        try {
                            args = objectMapper.convertValue(argsNode,
                                    new TypeReference<Map<String, Object>>() {});
                        } catch (Exception ignored) {
                            // best-effort; leave args empty if conversion fails
                        }
                    }
                    onChunk.accept(new GeminiStreamChunk.FunctionCall(name, args));
                }
            }
        }

        String finishReason = candidate.path("finishReason").asText("");
        if (!finishReason.isEmpty()) {
            if (BAD_FINISH_REASONS.contains(finishReason)) {
                throw new GeminiException("Gemini blocked content with finishReason=" + finishReason);
            }
            finalFinishReason.set(finishReason);
        }
    }

    /**
     * Calls Gemini and retries on transient upstream errors (overload /
     * rate limit / gateway). Lite models in particular routinely return
     * 503 under load — without retry the user sees AI_UPSTREAM_ERROR
     * on what would otherwise be a successful generation seconds later.
     */
    private String callWithRetry(Map<String, Object> body) {
        HttpStatusCodeException lastRetryable = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return geminiRestClient.post()
                        .uri("/v1beta/models/{model}:generateContent", properties.getModel())
                        .header("x-goog-api-key", properties.getKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if (RETRYABLE_STATUSES.contains(status) && attempt < MAX_ATTEMPTS) {
                    long backoffMs = 500L * (long) Math.pow(2, attempt - 1); // 500, 1000, 2000ms
                    log.warn("Gemini call status={} attempt={}/{} — retrying in {}ms",
                            e.getStatusCode(), attempt, MAX_ATTEMPTS, backoffMs);
                    sleepQuietly(backoffMs);
                    lastRetryable = e;
                    continue;
                }
                log.warn("Gemini call failed status={}", e.getStatusCode());
                throw new GeminiException("Gemini upstream HTTP error: " + e.getStatusCode());
            } catch (Exception e) {
                log.warn("Gemini call failed: {}", e.getClass().getSimpleName());
                throw new GeminiException("Gemini call failed");
            }
        }
        // Exhausted retries on a retryable status — surface the last one.
        log.warn("Gemini exhausted retries, last status={}",
                lastRetryable != null ? lastRetryable.getStatusCode() : "unknown");
        throw new GeminiException("Gemini upstream busy after "
                + MAX_ATTEMPTS + " attempts; try again in a moment.");
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cancellation handle passed into {@link #streamGenerate}. Callers
     * (e.g. the SSE controller) flip it to {@code cancelled = true} when
     * the downstream client disconnects, and the stream loop bails out at
     * the next chunk boundary.
     *
     * <p>Not a record — kept mutable so a single instance can be observed
     * from multiple threads. Volatile is sufficient: we only need
     * eventually-consistent visibility of the flag.
     */
    public static final class StreamCancelHandle {
        private volatile boolean cancelled = false;

        public boolean isCancelled() {
            return cancelled;
        }

        public void cancel() {
            this.cancelled = true;
        }
    }
}
