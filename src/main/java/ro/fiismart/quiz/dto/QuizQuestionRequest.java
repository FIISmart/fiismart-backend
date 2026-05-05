package ro.fiismart.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class QuizQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String text;

    private String type = "multiple_choice";
    private int points = 1;
    private List<String> options;

    @Min(value = 0, message = "Correct index must be >= 0")
    private int correctIdx;

    private String correctText;
    private String explanation;
}
