package com.fiismart.backend.course.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateQuizRequest {

    @NotBlank(message = "Quiz title is required")
    private String title;

    private int passingScore = 70;
    private int timeLimit = 30;
    private boolean shuffleQuestions;

    @NotEmpty(message = "At least one question is required")
    @Valid
    private List<QuizQuestionRequest> questions;
}
