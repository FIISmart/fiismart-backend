package ro.fiismart.ai.service;

import java.util.List;
import java.util.Map;

public final class PdfAiPrompts {

    private PdfAiPrompts() {}

    public static String buildPrompt(int questionCount, String language) {
        if ("en".equalsIgnoreCase(language)) {
            return "Read the attached PDF. Generate: "
                    + "(1) a summary in English of 4-8 paragraphs in Markdown, "
                    + "without titles or headings. "
                    + "(2) " + questionCount + " multiple-choice questions, "
                    + "each with exactly 4 options and a single correct answer. "
                    + "Use the specified JSON schema. "
                    + "Do not invent information that does not appear in the PDF.";
        }
        return "Citeste PDF-ul atasat. Genereaza: "
                + "(1) un rezumat in romana de 4-8 paragrafe in Markdown, "
                + "fara titluri sau heading-uri. "
                + "(2) " + questionCount + " intrebari de tip multiple-choice, "
                + "fiecare cu exact 4 optiuni si un singur raspuns corect. "
                + "Foloseste schema JSON specificata. "
                + "Nu inventa informatii care nu apar in PDF.";
    }

    public static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "summary", Map.of("type", "string"),
                    "quiz", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "title", Map.of("type", "string"),
                                    "passingScore", Map.of("type", "integer"),
                                    "timeLimit", Map.of("type", "integer"),
                                    "questions", Map.of(
                                            "type", "array",
                                            "items", Map.of(
                                                    "type", "object",
                                                    "properties", Map.of(
                                                            "text", Map.of("type", "string"),
                                                            "type", Map.of(
                                                                    "type", "string",
                                                                    "enum", List.of("multiple_choice")
                                                            ),
                                                            "options", Map.of(
                                                                    "type", "array",
                                                                    "items", Map.of("type", "string"),
                                                                    "minItems", 4,
                                                                    "maxItems", 4
                                                            ),
                                                            "correctIdx", Map.of(
                                                                    "type", "integer",
                                                                    "minimum", 0,
                                                                    "maximum", 3
                                                            ),
                                                            "explanation", Map.of("type", "string")
                                                    ),
                                                    "required", List.of("text", "type", "options", "correctIdx")
                                            ),
                                            "minItems", 1
                                    )
                            ),
                            "required", List.of("title", "questions")
                    )
            ),
            "required", List.of("summary", "quiz")
    );
}
