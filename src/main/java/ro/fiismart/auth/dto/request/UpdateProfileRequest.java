package ro.fiismart.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String lastName;

    @Size(max = 160)
    private String displayName;

    @Size(max = 40)
    private String phone;

    @Size(max = 1000)
    private String bio;

    @Size(max = 500)
    private String avatarUrl;
}
