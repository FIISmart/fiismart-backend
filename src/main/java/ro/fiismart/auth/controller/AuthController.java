package ro.fiismart.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.auth.dto.request.*;
import ro.fiismart.auth.dto.response.AuthResponse;
import ro.fiismart.auth.dto.response.OAuthExchangeResponse;
import ro.fiismart.auth.dto.response.UserResponse;
import ro.fiismart.auth.service.CognitoAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CognitoAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(201).body(Map.of(
                "message", "Registration successful. Please check your email for a verification code."
        ));
    }

    /** Alias /signup → /register pentru compatibilitate cu frontul */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(201).body(Map.of(
                "message", "Registration successful. Please check your email for a verification code."
        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req);
        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully. You can now sign in."
        ));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        authService.resendVerificationCode(req);
        return ResponseEntity.ok(Map.of(
                "message", "Verification code resent. Please check your email."
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    /**
     * Proxy the Cognito OAuth2 authorization-code exchange. The FE redirects
     * to the Cognito Hosted UI for federated sign-in (Google), gets a code
     * back at /auth/callback, then POSTs {code, codeVerifier, redirectUri}
     * here. We add the client_secret server-side and return the tokens.
     */
    @PostMapping("/oauth/exchange")
    public ResponseEntity<?> oauthExchange(@Valid @RequestBody OAuthExchangeRequest req) {
        try {
            return ResponseEntity.ok(authService.exchangeOAuthCode(req));
        } catch (CognitoAuthService.OAuthExchangeException e) {
            // Pass Cognito's status + JSON error body through (e.g. invalid_grant,
            // expired_code) so the FE can show a meaningful message.
            return ResponseEntity.status(e.getStatus())
                    .header("Content-Type", "application/json")
                    .body(e.getBody());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(Map.of(
                "message", "If that email is registered, a password reset code has been sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully. You can now sign in."
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(extractBearerToken(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(authService.getMe(userId));
    }

    /**
     * Utilizatorii federați (Google) fără rol selectat apelează acest endpoint
     * după ce aleg STUDENT sau PROFESSOR în pagina de Finalizare Profil.
     * Necesită JWT valid (@AuthenticationPrincipal → MongoDB user ID).
     */
    @PostMapping("/assign-role")
    public ResponseEntity<UserResponse> assignRole(
            @Valid @RequestBody AssignRoleRequest req,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(authService.assignRole(userId, req.getRole(), req.getFirstName(), req.getLastName()));
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or malformed Authorization header");
        }
        return header.substring(7);
    }
}
