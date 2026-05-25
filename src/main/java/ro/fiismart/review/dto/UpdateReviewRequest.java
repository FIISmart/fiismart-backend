package ro.fiismart.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateReviewRequest {

    @NotBlank
    private String body;

    @Min(1) @Max(5)
    private Integer stars;
}