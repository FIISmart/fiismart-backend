package ro.fiismart.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Toate valorile vin din application.properties / variabile de mediu.
 * Nu există valori hardcodate în cod — tot ce e blank înseamnă că variabila
 * de mediu corespunzătoare nu a fost setată.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws.cognito")
public class CognitoProperties {

    private String region = "";
    private String userPoolId = "";
    private String clientId = "";
    private String clientSecret = "";
    private String hostedUiDomain = "";
    private String redirectUri = "http://localhost:3000/auth/callback";
    private String spaClientId = "";

    public List<String> getAllowedClientIds() {
        List<String> ids = new java.util.ArrayList<>();
        if (clientId != null && !clientId.isBlank()) ids.add(clientId);
        if (spaClientId != null && !spaClientId.isBlank()) ids.add(spaClientId);
        return ids;
    }

    public String getJwksUri() {
        return String.format(
            "https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
            region, userPoolId
        );
    }

    public String getIssuerUri() {
        return String.format(
            "https://cognito-idp.%s.amazonaws.com/%s",
            region, userPoolId
        );
    }
}
