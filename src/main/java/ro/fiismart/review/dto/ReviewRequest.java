package ro.fiismart.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewRequest {

    private String studentId;

    @NotBlank
    private String courseId;

    @Min(1) @Max(5)
    private int stars;

    private String body;
}
