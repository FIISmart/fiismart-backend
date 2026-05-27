package ro.fiismart.tutors.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MentorMessageRequest {
    @NotBlank
    @Size(max = 4000)
    private String text;
}
