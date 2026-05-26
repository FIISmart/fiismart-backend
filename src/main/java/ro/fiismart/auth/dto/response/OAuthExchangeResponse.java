package ro.fiismart.auth.dto.response;

/**
 * Tokens returned from the Cognito OAuth2 /oauth2/token endpoint after the
 * BE-side authorization-code exchange. Shape mirrors what the FE used to
 * receive directly when it called Cognito itself.
 */
public record OAuthExchangeResponse(
        String accessToken,
        String idToken,
        String refreshToken,
        long expiresIn
) {}
