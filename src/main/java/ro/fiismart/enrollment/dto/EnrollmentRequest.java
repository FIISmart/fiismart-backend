package ro.fiismart.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnrollmentRequest {

    @NotBlank
    private String studentId;

    @NotBlank
    private String courseId;
}
