package ro.fiismart.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateEnrollmentStatusRequest {

    @NotBlank
    @Pattern(regexp = "enrolled|completed|dropped")
    private String status;
}