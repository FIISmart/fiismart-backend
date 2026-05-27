package ro.fiismart.tutors.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TutorRequestCreateRequest {
    @NotBlank
    private String tutorId;
    private String message;
}
