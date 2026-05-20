package ro.fiismart.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import ro.fiismart.ai.config.GeminiProperties;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
}
