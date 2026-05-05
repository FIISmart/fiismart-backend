package ro.fiismart.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuizRequest {

    private String courseId;

    @NotBlank(message = "Quiz title is required")
    private String title;

    private int passingScore = 70;
    private int timeLimit = 30;
    private boolean shuffleQuestions;
}
