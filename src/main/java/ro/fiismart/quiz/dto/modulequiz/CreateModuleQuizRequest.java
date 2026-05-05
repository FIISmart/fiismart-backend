package ro.fiismart.quiz.dto.modulequiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateModuleQuizRequest {

    @NotBlank(message = "Quiz title is required")
    private String title;

    private int passingScore = 70;
    private int timeLimit = 30;
    private boolean shuffleQuestions;

    @Valid
    private List<ModuleQuizQuestionRequest> questions;
}
