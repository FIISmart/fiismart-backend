package ro.fiismart.chat.tools;

/**
 * Validated input for {@link CourseBuildOrchestrator#build}. Bounds are
 * enforced upstream in {@code ChatToolHandler} before this record is
 * constructed — by the time we reach the orchestrator the values are
 * guaranteed in-range.
 *
 * <p>{@code language} is a free-form ISO-like tag ("ro", "en") forwarded
 * verbatim to the model; not validated against a fixed allowlist so
 * future languages don't require a server change.
 */
public record BuildCourseSpec(
        String subject,
        String audience,
        int moduleCount,
        int lecturesPerModule,
        int questionsPerQuiz,
        boolean includeQuizzes,
        String language
) {
}
