package ro.fiismart.quizattempt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import ro.fiismart.common.model.Answer;

import java.util.List;

/**
 * Submission payload for a quiz attempt.
 *
 * <p>Note: {@link #score}, {@link #passed}, and {@link #studentId} are <b>ignored</b>
 * by the server. {@code score}/{@code passed} are computed server-side in
 * {@link ro.fiismart.quizattempt.service.QuizAttemptService} from {@link #answers}
 * against the canonical quiz definition. {@code studentId} is derived from the
 * authenticated principal at the controller layer. The fields remain on the DTO
 * only for backwards compatibility with older clients.
 */
@Data
public class QuizAttemptRequest {

    @NotBlank
    private String quizId;

    @NotBlank
    private String courseId;

    /** @deprecated ignored server-side; derived from authenticated principal. */
    @Deprecated
    private String studentId;

    /** @deprecated ignored server-side; scored from {@link #answers}. */
    @Deprecated
    private Integer score;

    /** @deprecated ignored server-side; scored from {@link #answers}. */
    @Deprecated
    private Boolean passed;

    private int timeTakenSecs;
    private List<Answer> answers;
}
