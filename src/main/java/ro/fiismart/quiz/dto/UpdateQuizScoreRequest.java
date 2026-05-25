package ro.fiismart.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateQuizScoreRequest {

    @Min(0) @Max(100)
    private Integer passingScore;
}