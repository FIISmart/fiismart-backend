package ro.fiismart.tutors.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TutorRequestStatusUpdateRequest {
    @Pattern(regexp = "(?i)pending|accepted|declined|resolved", message = "Status invalid.")
    private String status;
}
