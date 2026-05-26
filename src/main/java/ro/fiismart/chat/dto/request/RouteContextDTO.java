package ro.fiismart.chat.dto.request;

/**
 * Snapshot of the frontend route at the moment the user sent a message.
 *
 * <p>Used by {@code ChatContextBuilder} to inject "you are on page X / in
 * lecture Y" into the system prompt so the assistant can answer in context
 * without us having to maintain a separate active-page state on the server.
 *
 * <p>All fields except {@code route} are nullable — e.g. a user typing from
 * the dashboard has no active course/lecture/quiz.
 */
public record RouteContextDTO(
        String route,
        String lectureId,
        String courseId,
        String quizId
) {
}
