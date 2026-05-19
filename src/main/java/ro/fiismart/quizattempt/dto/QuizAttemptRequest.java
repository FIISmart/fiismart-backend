package ro.fiismart.quizattempt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import ro.fiismart.common.model.Answer;

import java.util.List;

@Data
public class QuizAttemptRequest {

    @NotBlank
    private String quizId;

    @NotBlank
    private String courseId;

    private String studentId;

    private int score;
    private boolean passed;
    private int timeTakenSecs;
    private List<Answer> answers;
}
