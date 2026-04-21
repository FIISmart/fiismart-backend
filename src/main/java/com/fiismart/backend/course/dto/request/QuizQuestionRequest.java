package com.fiismart.backend.course.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class QuizQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String text;

    /** "multiple_choice" or "written". */
    private String type = "multiple_choice";
    private int points = 1;

    /** Required for multiple_choice, may be empty for written. */
    private List<String> options;

    /** Correct option index — only used for multiple_choice. */
    @Min(value = 0, message = "Correct index must be >= 0")
    private int correctIdx;

    /** Keyword-based correct answer — only used for written. */
    private String correctText;

    private String explanation;
}
