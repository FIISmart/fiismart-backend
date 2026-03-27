package com.fiismart.backend.course.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class QuizQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String text;

    private String type = "multiple_choice";
    private int points = 1;

    @NotEmpty(message = "At least two options are required")
    private List<String> options;

    @Min(value = 0, message = "Correct index must be >= 0")
    private int correctIdx;

    private String explanation;
}
