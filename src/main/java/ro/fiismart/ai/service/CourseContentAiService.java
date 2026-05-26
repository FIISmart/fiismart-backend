package ro.fiismart.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.fiismart.ai.client.GeminiClient;
import ro.fiismart.ai.client.GeminiException;
import ro.fiismart.ai.dto.response.GeneratedCourseDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single-shot full-course generator used by the {@code buildFullCourse}
 * chat tool. One Gemini call returns the entire course tree (title,
 * description, tags, modules → lectures + optional module quiz) so the
 * model can keep cross-module coherence — escalating difficulty, no
 * duplicate lecture titles, consistent audience tone.
 *
 * <p>This service is intentionally <em>not</em> a streamer: the FE
 * progress UI (powered by {@code CourseBuildOrchestrator}) reports
 * progress on the <em>persist</em> stage, not the generate stage —
 * Gemini's JSON-mode endpoint doesn't stream meaningful intermediate
 * structure anyway.
 *
 * <p>Retries on 429/5xx are inherited from {@link GeminiClient}. A
 * malformed response (failed Jackson parse) is surfaced as
 * {@link GeminiException}; the caller decides whether to roll back the
 * partial course or leave it as a draft.
 */
@Service
public class CourseContentAiService {

    private static final Logger log = LoggerFactory.getLogger(CourseContentAiService.class);

    /** Hard cap on per-lecture markdown content. Communicated to the
     *  model via the schema's description; we don't trim server-side
     *  because partial markdown is worse than verbose markdown. */
    private static final int MAX_LECTURE_CONTENT_CHARS = 4000;

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public CourseContentAiService(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a full course tree.
     *
     * @param subject            short topic the course should teach
     * @param audience           who the course is for (e.g. "studenti anul 1")
     * @param moduleCount        how many modules to produce (caller has clamped)
     * @param lecturesPerModule  lectures per module (caller has clamped)
     * @param questionsPerQuiz   questions per module quiz (used only when
     *                           {@code includeQuizzes=true})
     * @param includeQuizzes     when {@code true}, each module gets a quiz
     * @param language           ISO code, defaults to "ro" upstream
     */
    public GeneratedCourseDTO generate(
            String subject,
            String audience,
            int moduleCount,
            int lecturesPerModule,
            int questionsPerQuiz,
            boolean includeQuizzes,
            String language) {

        String prompt = buildPrompt(subject, audience, moduleCount,
                lecturesPerModule, questionsPerQuiz, includeQuizzes, language);
        Map<String, Object> schema = buildSchema(includeQuizzes);

        log.info("CourseContentAiService.generate subject='{}' modules={} lectures={} quizzes={}",
                truncate(subject, 80), moduleCount, lecturesPerModule, includeQuizzes);

        String json = geminiClient.generateJson(prompt, schema);
        try {
            return objectMapper.readValue(json, GeneratedCourseDTO.class);
        } catch (Exception e) {
            log.warn("CourseContentAiService: failed to parse Gemini response: {}",
                    e.getClass().getSimpleName());
            throw new GeminiException("Gemini returned invalid course structure", e);
        }
    }

    private static String buildPrompt(String subject, String audience,
                                      int moduleCount, int lecturesPerModule,
                                      int questionsPerQuiz, boolean includeQuizzes,
                                      String language) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Genereaza un curs educational complet, in limba ");
        sb.append(language == null || language.isBlank() ? "romana" : language).append(".\n");
        sb.append("Subiect: \"").append(subject).append("\".\n");
        sb.append("Public tinta: ").append(audience).append(".\n");
        sb.append("Structura ceruta: ").append(moduleCount).append(" module, fiecare cu ")
                .append(lecturesPerModule).append(" lectii.\n");
        sb.append("Continutul fiecarei lectii trebuie sa fie un text didactic in markdown ")
                .append("(maxim ").append(MAX_LECTURE_CONTENT_CHARS).append(" caractere), ")
                .append("structurat cu titluri, paragrafe si exemple. Estimeaza durationSecs ")
                .append("(intre 180 si 900 secunde, in functie de complexitate).\n");
        if (includeQuizzes) {
            sb.append("Pentru fiecare modul, genereaza si un quiz cu ").append(questionsPerQuiz)
                    .append(" intrebari multiple_choice cu exact 4 optiuni si o singura ")
                    .append("corecta (correctIdx in {0,1,2,3}). passingScore intre 60 si 80.\n");
        } else {
            sb.append("Nu include quiz pentru module.\n");
        }
        sb.append("Asigura-te ca lectiile au titluri distincte si urmaresc o progresie logica. ")
                .append("Returneaza JSON conform schemei, fara text in afara JSON-ului.");
        return sb.toString();
    }

    /**
     * JSON schema mirroring {@link GeneratedCourseDTO}. The
     * {@code description} fields are hints to the model — they're not
     * enforced server-side so we still treat unknown lengths leniently.
     */
    private static Map<String, Object> buildSchema(boolean includeQuizzes) {
        Map<String, Object> lectureProps = new LinkedHashMap<>();
        lectureProps.put("title", Map.of("type", "string"));
        lectureProps.put("content", Map.of(
                "type", "string",
                "description", "Continutul lectiei in markdown, maxim "
                        + MAX_LECTURE_CONTENT_CHARS + " caractere."
        ));
        lectureProps.put("durationSecs", Map.of(
                "type", "integer",
                "minimum", 60,
                "maximum", 1800
        ));
        Map<String, Object> lectureSchema = Map.of(
                "type", "object",
                "properties", lectureProps,
                "required", List.of("title", "content")
        );

        Map<String, Object> questionProps = new LinkedHashMap<>();
        questionProps.put("text", Map.of("type", "string"));
        questionProps.put("type", Map.of(
                "type", "string",
                "enum", List.of("multiple_choice")
        ));
        questionProps.put("options", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "minItems", 4,
                "maxItems", 4
        ));
        questionProps.put("correctIdx", Map.of("type", "integer", "minimum", 0, "maximum", 3));
        questionProps.put("explanation", Map.of("type", "string"));
        Map<String, Object> questionSchema = Map.of(
                "type", "object",
                "properties", questionProps,
                "required", List.of("text", "type", "options", "correctIdx")
        );

        Map<String, Object> quizProps = new LinkedHashMap<>();
        quizProps.put("title", Map.of("type", "string"));
        quizProps.put("passingScore", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        quizProps.put("questions", Map.of(
                "type", "array",
                "items", questionSchema,
                "minItems", 1
        ));
        Map<String, Object> quizSchema = Map.of(
                "type", "object",
                "properties", quizProps,
                "required", List.of("title", "questions")
        );

        Map<String, Object> moduleProps = new LinkedHashMap<>();
        moduleProps.put("title", Map.of("type", "string"));
        moduleProps.put("description", Map.of("type", "string"));
        moduleProps.put("lectures", Map.of(
                "type", "array",
                "items", lectureSchema,
                "minItems", 1
        ));
        if (includeQuizzes) {
            moduleProps.put("quiz", quizSchema);
        }
        Map<String, Object> moduleSchema = Map.of(
                "type", "object",
                "properties", moduleProps,
                "required", includeQuizzes
                        ? List.of("title", "lectures", "quiz")
                        : List.of("title", "lectures")
        );

        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("title", Map.of("type", "string"));
        rootProps.put("description", Map.of("type", "string"));
        rootProps.put("language", Map.of("type", "string"));
        rootProps.put("tags", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "maxItems", 10
        ));
        rootProps.put("modules", Map.of(
                "type", "array",
                "items", moduleSchema,
                "minItems", 1
        ));

        return Map.of(
                "type", "object",
                "properties", rootProps,
                "required", List.of("title", "description", "modules")
        );
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
