package ro.fiismart.chat.tools;

/**
 * Outcome of a successful (full or partial) {@code buildFullCourse}
 * dispatch. Returned as the tool_result payload so the FE can deep-link
 * straight to the new course in the builder.
 *
 * <p>Counts reflect what was <em>actually persisted</em> — not what was
 * requested — so on partial failure the chat reply can be honest about
 * how far the build got before stopping.
 */
public record BuildCourseResult(
        String courseId,
        String title,
        int moduleCount,
        int lectureCount,
        int quizCount
) {
}
