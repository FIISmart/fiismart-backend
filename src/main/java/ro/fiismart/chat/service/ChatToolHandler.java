package ro.fiismart.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.fiismart.ai.client.GeminiClient;
import ro.fiismart.ai.client.GeminiException;
import ro.fiismart.ai.dto.response.AiQuizDraftDTO;
import ro.fiismart.chat.dto.response.CourseDraftDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dispatches Gemini tool-calls to the corresponding draft-builders.
 *
 * <p>The chatbot only exposes <em>generative</em> tools (no side effects):
 * each handler runs a focused, non-streaming Gemini JSON call and returns
 * the parsed DTO. We deliberately avoid letting the model invoke tools
 * that mutate server state — drafts are previewed and accepted by the user
 * via the FE before anything is persisted.
 *
 * <p>Argument validation is strict on bounds the model can hallucinate
 * (counts, enums) and lenient elsewhere — extra unknown fields in
 * {@code args} are ignored rather than rejected so a slightly drifted
 * Gemini output doesn't break the chat turn.
 */
@Service
public class ChatToolHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatToolHandler.class);

    static final String TOOL_CREATE_QUIZ_DRAFT = "createQuizDraft";
    static final String TOOL_CREATE_COURSE_DRAFT = "createCourseDraft";

    private static final int MIN_QUIZ_QUESTIONS = 3;
    private static final int MAX_QUIZ_QUESTIONS = 20;
    private static final int MIN_MODULES = 3;
    private static final int MAX_MODULES = 10;
    private static final Set<String> DIFFICULTIES = Set.of("easy", "medium", "hard");

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public ChatToolHandler(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Routes a single tool call by name. Unknown tool names throw
     * {@link IllegalArgumentException} — handled by
     * {@code GlobalExceptionHandler} as a 400 (the global handler maps
     * IAE → 400). {@code userId} is currently informational (logged) since
     * draft generation has no side effects; it's accepted in the signature
     * so adding side-effecting tools later doesn't break callers.
     */
    public Object dispatch(String toolName, Map<String, Object> args, String userId) {
        log.info("Chat tool dispatch user={} tool={} argKeys={}",
                userId, toolName, args == null ? "[]" : args.keySet());
        return switch (toolName) {
            case TOOL_CREATE_QUIZ_DRAFT -> buildQuizDraft(args);
            case TOOL_CREATE_COURSE_DRAFT -> buildCourseDraft(args);
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    // ── createQuizDraft ──────────────────────────────────────────────────

    AiQuizDraftDTO buildQuizDraft(Map<String, Object> args) {
        String topic = requireString(args, "topic");
        int questionCount = clampInt(args, "questionCount", MIN_QUIZ_QUESTIONS, MAX_QUIZ_QUESTIONS, 5);
        String difficulty = optionalEnum(args, "difficulty", DIFFICULTIES, "medium");
        boolean includeFreeText = optionalBool(args, "includeFreeText", false);

        String prompt = quizDraftPrompt(topic, questionCount, difficulty, includeFreeText);
        Map<String, Object> schema = quizDraftSchema(includeFreeText);

        String json = geminiClient.generateJson(prompt, schema);
        try {
            return objectMapper.readValue(json, AiQuizDraftDTO.class);
        } catch (Exception e) {
            log.warn("createQuizDraft: failed to parse Gemini response: {}", e.getClass().getSimpleName());
            throw new GeminiException("Gemini returned invalid quiz draft structure", e);
        }
    }

    private static String quizDraftPrompt(String topic, int count, String difficulty, boolean includeFreeText) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("Genereaza un quiz educational in romana pe tema: \"").append(topic).append("\".\n");
        sb.append("Numar de intrebari: ").append(count).append(".\n");
        sb.append("Dificultate: ").append(difficulty).append(".\n");
        if (includeFreeText) {
            int freeTextCount = Math.max(1, Math.round(count * 0.3f));
            sb.append("Aproximativ ").append(freeTextCount)
                    .append(" intrebari (cca 30%) trebuie sa fie de tip free_text — text liber, ");
            sb.append("evaluate de AI. Pentru acestea, completeaza si sampleAnswer (raspuns model), ");
            sb.append("keyConcepts (3-5 concepte cheie obligatorii) si passThreshold (intre 60 si 80).\n");
            sb.append("Restul intrebarilor sa fie multiple_choice cu 4 optiuni si o singura corecta.\n");
        } else {
            sb.append("Toate intrebarile sunt multiple_choice cu 4 optiuni si o singura corecta.\n");
        }
        sb.append("Returneaza JSON conform schemei. Nu adauga text in afara JSON-ului.");
        return sb.toString();
    }

    /**
     * JSON schema for the quiz draft. Mirrors {@code AiQuizDraftDTO} +
     * {@code AiQuizQuestionDTO} with optional free-text extension fields
     * accepted (and Gemini-side enforced as required when applicable).
     */
    private static Map<String, Object> quizDraftSchema(boolean includeFreeText) {
        Map<String, Object> questionProps = new java.util.LinkedHashMap<>();
        questionProps.put("text", Map.of("type", "string"));
        questionProps.put("type", Map.of(
                "type", "string",
                "enum", includeFreeText
                        ? List.of("multiple_choice", "free_text")
                        : List.of("multiple_choice")
        ));
        questionProps.put("options", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "minItems", 0,
                "maxItems", 4
        ));
        questionProps.put("correctIdx", Map.of("type", "integer", "minimum", 0, "maximum", 3));
        questionProps.put("explanation", Map.of("type", "string"));
        if (includeFreeText) {
            questionProps.put("sampleAnswer", Map.of("type", "string"));
            questionProps.put("keyConcepts", Map.of(
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "minItems", 1
            ));
            questionProps.put("passThreshold", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        }

        Map<String, Object> questionSchema = Map.of(
                "type", "object",
                "properties", questionProps,
                "required", List.of("text", "type")
        );

        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "passingScore", Map.of("type", "integer"),
                        "timeLimit", Map.of("type", "integer"),
                        "questions", Map.of(
                                "type", "array",
                                "items", questionSchema,
                                "minItems", 1
                        )
                ),
                "required", List.of("title", "questions")
        );
    }

    // ── createCourseDraft ────────────────────────────────────────────────

    CourseDraftDTO buildCourseDraft(Map<String, Object> args) {
        String subject = requireString(args, "subject");
        String audience = requireString(args, "audience");
        int moduleCount = clampInt(args, "moduleCount", MIN_MODULES, MAX_MODULES, 4);

        String prompt = courseDraftPrompt(subject, audience, moduleCount);
        Map<String, Object> schema = courseDraftSchema();

        String json = geminiClient.generateJson(prompt, schema);
        try {
            return objectMapper.readValue(json, CourseDraftDTO.class);
        } catch (Exception e) {
            log.warn("createCourseDraft: failed to parse Gemini response: {}", e.getClass().getSimpleName());
            throw new GeminiException("Gemini returned invalid course draft structure", e);
        }
    }

    private static String courseDraftPrompt(String subject, String audience, int moduleCount) {
        return "Genereaza schema unui curs educational in romana pe tema: \"" + subject + "\".\n"
                + "Public tinta: " + audience + ".\n"
                + "Numar module: " + moduleCount + ", fiecare cu 3-6 lectii. "
                + "Returneaza titlu, descriere scurta, si lista de module — fiecare modul cu titlu si "
                + "lectureTitles (titlurile lectiilor, fara continut). Returneaza JSON conform schemei.";
    }

    private static Map<String, Object> courseDraftSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "description", Map.of("type", "string"),
                        "modules", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "title", Map.of("type", "string"),
                                                "lectureTitles", Map.of(
                                                        "type", "array",
                                                        "items", Map.of("type", "string"),
                                                        "minItems", 1
                                                )
                                        ),
                                        "required", List.of("title", "lectureTitles")
                                ),
                                "minItems", 1
                        )
                ),
                "required", List.of("title", "modules")
        );
    }

    // ── arg helpers ──────────────────────────────────────────────────────

    private static String requireString(Map<String, Object> args, String key) {
        if (args == null) {
            throw new IllegalArgumentException("Missing tool arg: " + key);
        }
        Object v = args.get(key);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Tool arg '" + key + "' must be a non-empty string");
        }
        // Cap length to keep prompt size bounded — the model occasionally
        // hands us paragraphs in args; 240 chars is enough for any topic
        // / audience description.
        return s.length() > 240 ? s.substring(0, 240) : s;
    }

    private static int clampInt(Map<String, Object> args, String key, int min, int max, int fallback) {
        if (args == null || !args.containsKey(key)) {
            return fallback;
        }
        Object v = args.get(key);
        int parsed;
        if (v instanceof Number n) {
            parsed = n.intValue();
        } else if (v instanceof String s) {
            try {
                parsed = Integer.parseInt(s.trim());
            } catch (NumberFormatException nfe) {
                return fallback;
            }
        } else {
            return fallback;
        }
        if (parsed < min) return min;
        if (parsed > max) return max;
        return parsed;
    }

    private static String optionalEnum(Map<String, Object> args, String key, Set<String> allowed, String fallback) {
        if (args == null) return fallback;
        Object v = args.get(key);
        if (v instanceof String s && allowed.contains(s.toLowerCase())) {
            return s.toLowerCase();
        }
        return fallback;
    }

    private static boolean optionalBool(Map<String, Object> args, String key, boolean fallback) {
        if (args == null) return fallback;
        Object v = args.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return fallback;
    }
}
