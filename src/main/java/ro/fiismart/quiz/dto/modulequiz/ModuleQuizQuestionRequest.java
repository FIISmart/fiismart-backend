package ro.fiismart.quiz.dto.modulequiz;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ModuleQuizQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String text;

    /** Optional image URL displayed alongside the question. */
    private String imageUrl;

    private String type = "multiple_choice";
    private int points = 1;

    @NotEmpty(message = "At least two options are required")
    private List<String> options;

    @Min(value = 0, message = "Correct index must be >= 0")
    private int correctIdx;

    private String explanation;
}
