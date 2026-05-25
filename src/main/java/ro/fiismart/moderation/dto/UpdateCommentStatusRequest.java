package ro.fiismart.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateCommentStatusRequest {

    @NotBlank
    @Pattern(regexp = "approved|rejected|pending")
    private String status;
}