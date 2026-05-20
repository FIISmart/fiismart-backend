package ro.fiismart.quizattempt.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/v1/quiz-attempts/start}.
 *
 * <p>Note: deliberately does NOT include a {@code courseId}. The previous
 * iteration accepted courseId from the client without verifying it matched
 * {@code quiz.courseId}, which let a caller mis-attribute an attempt to an
 * arbitrary course. The server now derives {@code courseId} from the quiz.</p>
 */
public record StartQuizAttemptRequest(@NotBlank String quizId) {}
