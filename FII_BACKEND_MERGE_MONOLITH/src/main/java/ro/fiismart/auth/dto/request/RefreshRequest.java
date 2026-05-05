package ro.fiismart.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    // Cognito SECRET_HASH = HMAC(email + clientId) — username-ul din pool este emailul.
    @NotBlank(message = "Email is required")
    private String email;
}
