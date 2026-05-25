package ro.fiismart.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateQuizTitleRequest {

    @NotBlank
    private String title;
}