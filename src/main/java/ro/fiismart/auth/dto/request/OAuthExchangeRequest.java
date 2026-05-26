package ro.fiismart.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to exchange a Cognito OAuth2 authorization code (from the Hosted UI
 * / federated IdP callback) for tokens. The BE owns the client_secret, so the
 * FE never has to bake it into its bundle.
 */
public class OAuthExchangeRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String codeVerifier;

    @NotBlank
    private String redirectUri;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCodeVerifier() { return codeVerifier; }
    public void setCodeVerifier(String codeVerifier) { this.codeVerifier = codeVerifier; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
}
