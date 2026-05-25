package ro.fiismart.common.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
            "invalid_token", "The audience does not match any allowed client ID", null
    );

    private final List<String> allowedClientIds;

    public JwtAudienceValidator(List<String> allowedClientIds) {
        this.allowedClientIds = allowedClientIds;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();
        if (audiences != null && audiences.stream().anyMatch(allowedClientIds::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        String clientId = jwt.getClaimAsString("client_id");
        if (clientId != null && allowedClientIds.contains(clientId)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
    }
}
