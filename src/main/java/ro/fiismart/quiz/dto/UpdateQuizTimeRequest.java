package ro.fiismart.quiz.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateQuizTimeRequest {

    @Min(0)
    private Integer timeLimitMinutes;
}