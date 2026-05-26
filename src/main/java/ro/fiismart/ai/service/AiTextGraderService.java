package ro.fiismart.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.fiismart.ai.client.GeminiClient;
import ro.fiismart.ai.client.GeminiException;
import ro.fiismart.ai.dto.response.FreeTextGradeResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Grades a student's free-text quiz answer against a professor-supplied
 * rubric (sample answer + required key concepts) using Gemini in strict
 * JSON-schema mode.
 *
 * <p>Failure policy: this service NEVER throws. When Gemini is unavailable
 * (after the client's own retry budget), or returns malformed output, it
 * logs a warning and returns a fallback
 * {@code FreeTextGradeResult(null, 0.0, "Evaluare AI esuata.", List.of())}.
 * That way a single transient AI failure cannot fail an entire quiz
 * submission — the question is simply marked incorrect (null score).</p>
 *
 * <p>Built without Lombok by design: keeps the AI surface area free of
 * generated-code surprises while we iterate on prompts.</p>
 */
@Service
public class AiTextGraderService {

    private static final Logger log = LoggerFactory.getLogger(AiTextGraderService.class);

    /** Fallback returned when grading fails — keeps the submission progressing. */
    static final FreeTextGradeResult FAILURE_RESULT =
            new FreeTextGradeResult(null, 0.0, "Evaluare AI esuata.", List.of());

    /** Strict JSON schema we ask Gemini to honour. Centralised so the prompt
     *  and the parser agree on the shape. */
    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "score", Map.of(
                            "type", "number",
                            "minimum", 0,
                            "maximum", 100
                    ),
                    "confidence", Map.of(
                            "type", "number",
                            "minimum", 0,
                            "maximum", 1
                    ),
                    "reasoning", Map.of("type", "string"),
                    "missingConcepts", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                    )
            ),
            "required", List.of("score", "confidence", "reasoning", "missingConcepts")
    );

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public AiTextGraderService(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Score one student answer against the rubric.
     *
     * @param question       the question text shown to the student
     * @param sampleAnswer   professor-provided reference answer
     * @param keyConcepts    required concepts (may be empty/null)
     * @param studentAnswer  what the student typed
     * @return a grade result; {@link #FAILURE_RESULT} on any failure path
     */
    public FreeTextGradeResult grade(
            String question,
            String sampleAnswer,
            List<String> keyConcepts,
            String studentAnswer) {

        if (studentAnswer == null || studentAnswer.isBlank()) {
            // Empty answers do not need an AI round-trip — and we want them
            // to fall under threshold deterministically.
            return new FreeTextGradeResult(
                    0.0,
                    1.0,
                    "Raspuns gol.",
                    keyConcepts == null ? List.of() : List.copyOf(keyConcepts));
        }

        String prompt = buildPrompt(
                safe(question),
                safe(sampleAnswer),
                keyConcepts == null ? List.of() : keyConcepts,
                safe(studentAnswer));

        String rawJson;
        try {
            rawJson = geminiClient.generateJson(prompt, RESPONSE_SCHEMA);
        } catch (GeminiException ge) {
            log.warn("AI grading failed (upstream): {}", ge.getMessage());
            return FAILURE_RESULT;
        } catch (Exception e) {
            log.warn("AI grading failed (unexpected {}): {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return FAILURE_RESULT;
        }

        return parseResponse(rawJson);
    }

    /**
     * Builds the Romanian rubric prompt. Kept as a static helper (no
     * instance state) so it stays easy to unit test independently.
     */
    static String buildPrompt(String question,
                              String sampleAnswer,
                              List<String> keyConcepts,
                              String studentAnswer) {

        String conceptsBlock;
        if (keyConcepts == null || keyConcepts.isEmpty()) {
            conceptsBlock = "(niciun concept obligatoriu specificat)";
        } else {
            StringBuilder sb = new StringBuilder();
            for (String c : keyConcepts) {
                if (c == null || c.isBlank()) continue;
                if (sb.length() > 0) sb.append(", ");
                sb.append(c.trim());
            }
            conceptsBlock = sb.length() == 0 ? "(niciun concept obligatoriu specificat)" : sb.toString();
        }

        return "Esti corector de examen. Evalueaza raspunsul studentului fata de raspunsul model si conceptele cheie.\n"
                + "Intrebare: " + question + "\n"
                + "Raspuns model: " + sampleAnswer + "\n"
                + "Concepte cheie obligatorii: " + conceptsBlock + "\n"
                + "Raspuns student: " + studentAnswer + "\n\n"
                + "Returneaza JSON strict conform schemei:\n"
                + "{\n"
                + "  \"score\": 0-100,\n"
                + "  \"confidence\": 0-1,\n"
                + "  \"reasoning\": \"scurt, in romana, 1-2 propozitii\",\n"
                + "  \"missingConcepts\": [\"concept lipsa\", ...]\n"
                + "}";
    }

    /**
     * Parses Gemini's response into a {@link FreeTextGradeResult}. Robust
     * to missing/extra fields: anything missing degrades to the failure
     * sentinel rather than throwing.
     */
    private FreeTextGradeResult parseResponse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("AI grading: empty response");
            return FAILURE_RESULT;
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode scoreNode = root.get("score");
            JsonNode confidenceNode = root.get("confidence");
            JsonNode reasoningNode = root.get("reasoning");
            JsonNode missingNode = root.get("missingConcepts");

            Double score = (scoreNode != null && scoreNode.isNumber()) ? scoreNode.asDouble() : null;
            Double confidence = (confidenceNode != null && confidenceNode.isNumber())
                    ? confidenceNode.asDouble()
                    : null;
            String reasoning = (reasoningNode != null && reasoningNode.isTextual())
                    ? reasoningNode.asText()
                    : "";

            List<String> missingConcepts = new ArrayList<>();
            if (missingNode != null && missingNode.isArray()) {
                missingNode.forEach(n -> {
                    if (n != null && n.isTextual() && !n.asText().isBlank()) {
                        missingConcepts.add(n.asText());
                    }
                });
            }

            // Defensive clamping — Gemini sometimes ignores schema bounds.
            if (score != null) {
                if (score < 0.0) score = 0.0;
                if (score > 100.0) score = 100.0;
            }
            if (confidence != null) {
                if (confidence < 0.0) confidence = 0.0;
                if (confidence > 1.0) confidence = 1.0;
            }

            // If the critical fields are missing, treat as failure.
            if (score == null || confidence == null) {
                log.warn("AI grading: missing score/confidence in response");
                return FAILURE_RESULT;
            }

            return new FreeTextGradeResult(score, confidence, reasoning, List.copyOf(missingConcepts));
        } catch (Exception e) {
            log.warn("AI grading: failed to parse response ({}): {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return FAILURE_RESULT;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
