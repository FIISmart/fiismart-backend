package ro.fiismart.quizattempt.dto;

import jakarta.validation.constraints.NotEmpty;
import ro.fiismart.common.model.Answer;

import java.util.List;

/**
 * Body of {@code POST /api/v1/quiz-attempts/{attemptId}/submit}.
 *
 * <p><b>Anti-cheat invariant:</b> deliberately does NOT include
 * {@code score} or {@code passed}. Those are computed server-side from
 * the submitted answers, the canonical {@code correctIdx}/{@code correctText}
 * on {@link ro.fiismart.common.model.ModuleQuiz}, and the elapsed time.
 * Accepting client-supplied score/passed would defeat the entire purpose
 * of this endpoint.</p>
 */
public record SubmitQuizAttemptRequest(@NotEmpty List<Answer> answers) {}
