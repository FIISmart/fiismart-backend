package ro.fiismart.quizattempt.dto;

import java.time.Instant;

/**
 * Returned by {@code POST /api/v1/quiz-attempts/start}. The FE uses
 * {@code timeLimitSeconds} (NOT minutes) to drive the countdown so it
 * can never accidentally treat the quiz's MINUTES-based
 * {@link ro.fiismart.common.model.ModuleQuiz#getTimeLimit()} as seconds.
 */
public record StartQuizAttemptResponse(
        String attemptId,
        Instant startedAt,
        int timeLimitSeconds,
        String status) {}
