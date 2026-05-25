package ro.fiismart.ai.dto.response;

import java.util.List;

/**
 * Result of an AI rubric-based grading pass for a free-text quiz answer.
 *
 * <p>All numeric fields are nullable to signal grading failure: when Gemini
 * is unavailable or returns malformed output, {@code AiTextGraderService}
 * returns {@code new FreeTextGradeResult(null, 0.0, "Evaluare AI esuata.", List.of())}
 * so the quiz submission keeps progressing instead of failing the whole attempt.</p>
 *
 * @param score            0-100, null when grading failed
 * @param confidence       0-1, null when grading failed (0.0 for the fallback)
 * @param reasoning        short Romanian text shown to the student
 * @param missingConcepts  concepts from the rubric that the student answer omitted
 */
public record FreeTextGradeResult(
        Double score,
        Double confidence,
        String reasoning,
        List<String> missingConcepts
) {}
