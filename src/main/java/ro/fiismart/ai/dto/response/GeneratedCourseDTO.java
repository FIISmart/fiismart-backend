package ro.fiismart.ai.dto.response;

import java.util.List;

/**
 * Top-level shape returned by {@code CourseContentAiService.generate(...)}.
 * One Gemini call produces the entire tree (title + description + tags +
 * modules → lectures + optional quiz) so the model can keep cross-module
 * coherence (e.g. no duplicate lecture titles, escalating difficulty).
 *
 * <p>The corresponding JSON schema constrains content sizes (lecture
 * body capped at 4000 chars) and quiz shape (multiple_choice with
 * 4 options) to keep the persisted course bounded.
 */
public record GeneratedCourseDTO(
        String title,
        String description,
        String language,
        List<String> tags,
        List<GeneratedModule> modules
) {
}
