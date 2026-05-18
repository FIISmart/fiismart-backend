package ro.fiismart.quiz.dto.modulequiz;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ModuleQuizQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String text;

    /** Optional image URL displayed alongside the question. */
    private String imageUrl;

    /** One of {@code "multiple_choice"} (default) or {@code "written"}. */
    private String type = "multiple_choice";
    private int points = 1;

    /** Required for {@code "multiple_choice"} questions; empty/null for {@code "written"}. */
    private List<String> options;

    /** Required for {@code "multiple_choice"} questions; null for {@code "written"}. */
    private Integer correctIdx;

    /** Required for {@code "written"} questions; null for {@code "multiple_choice"}. */
    private String correctText;

    private String explanation;
}
