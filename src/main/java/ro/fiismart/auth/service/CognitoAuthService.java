package ro.fiismart.auth.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.fiismart.auth.dto.UserRole;
import ro.fiismart.auth.dto.request.*;
import ro.fiismart.auth.dto.response.AuthResponse;
import ro.fiismart.auth.dto.response.UserResponse;
import ro.fiismart.common.config.CognitoProperties;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ro.fiismart.auth.dto.request.OAuthExchangeRequest;
import ro.fiismart.auth.dto.response.OAuthExchangeResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoAuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final UserRepository userRepository;
    private final CognitoProperties cognitoProperties;

    // ── Startup validation ────────────────────────────────────────────────────

    @PostConstruct
    public void logConfiguration() {
        log.info("=== Cognito Configuration ===");
        log.info("  Region:       {}", cognitoProperties.getRegion());
        log.info("  User Pool ID: {}", cognitoProperties.getUserPoolId());
        log.info("  Client ID:    {}", cognitoProperties.getClientId());
        log.info("  JWKS URI:     {}", cognitoProperties.getJwksUri());
        log.info("  Client secret configured: {}",
                cognitoProperties.getClientSecret() != null
                        && !cognitoProperties.getClientSecret().isBlank());
    }

    // ── SECRET_HASH helper ────────────────────────────────────────────────────
    // Cognito requires this when the App Client has a client_secret.
    // Formula: HmacSHA256(clientSecret, username + clientId), Base64-encoded.
    private String secretHash(String username) {
        String secret = cognitoProperties.getClientSecret();
        if (secret == null || secret.isBlank()) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(username.getBytes(StandardCharsets.UTF_8));
            byte[] raw = mac.doFinal(cognitoProperties.getClientId().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute Cognito SECRET_HASH", e);
        }
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    public void register(RegisterRequest req) {
        SignUpRequest.Builder builder = SignUpRequest.builder()
                .clientId(cognitoProperties.getClientId())
                .secretHash(secretHash(req.getEmail()))
                .username(req.getEmail())
                .password(req.getPassword())
                .userAttributes(
                        AttributeType.builder().name("email").value(req.getEmail()).build(),
                        AttributeType.builder().name("given_name").value(req.getFirstName()).build(),
                        AttributeType.builder().name("family_name").value(req.getLastName()).build(),
                        AttributeType.builder().name("name").value(req.getFirstName() + " " + req.getLastName()).build()
                );

        SignUpResponse signUp;
        try {
            signUp = cognitoClient.signUp(builder.build());
        } catch (NotAuthorizedException e) {
            log.error("Signup BLOCAT de Cognito — self-registration dezactivat sau App Client greșit. Mesaj: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Signup eroare: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }

        String sub       = signUp.userSub();
        String role      = req.getRole() == UserRole.PROFESSOR ? "professor" : "student";
        String groupName = role.toUpperCase(); // "PROFESSOR" sau "STUDENT"
        String email     = req.getEmail().toLowerCase();

        // Upsert: dacă există deja un document cu același email (e.g. rată anterioară de înregistrare),
        // actualizăm cognitoSub-ul — altfel creăm un document nou.
        java.util.Optional<User> existingByEmail = userRepository.findByEmail(email);
        User userToSave;
        if (existingByEmail.isPresent()) {
            userToSave = existingByEmail.get();
            userToSave.setCognitoSub(sub);
            userToSave.setRole(role);
            userToSave.setDisplayName(req.getFirstName() + " " + req.getLastName());
            log.info("Email {} exista deja în MongoDB — actualizez cognitoSub la: {}", email, sub);
        } else if (!userRepository.existsByCognitoSub(sub)) {
            userToSave = User.builder()
                    .cognitoSub(sub)
                    .email(email)
                    .displayName(req.getFirstName() + " " + req.getLastName())
                    .role(role)
                    .createdAt(new Date())
                    .banned(false)
                    .build();
        } else {
            // sub-ul există deja — nimic de făcut
            userToSave = null;
        }

        if (userToSave != null) {
            try {
                userRepository.save(userToSave);
                log.info("User salvat în MongoDB: {} (sub={})", email, sub);
            } catch (Exception dbEx) {
                log.error("Eroare salvare MongoDB pentru {} — rollback Cognito: {}", email, dbEx.getMessage());
                try {
                    cognitoClient.adminDeleteUser(AdminDeleteUserRequest.builder()
                            .userPoolId(cognitoProperties.getUserPoolId())
                            .username(req.getEmail())
                            .build());
                    log.info("Rollback Cognito reușit pentru: {}", email);
                } catch (Exception rollbackEx) {
                    log.error("Rollback Cognito EȘUAT pentru {} — cleanup manual necesar: {}",
                            email, rollbackEx.getMessage());
                }
                throw new RuntimeException("Eroare la salvarea contului. Încearcă din nou.", dbEx);
            }
        }

        try {
            cognitoClient.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(req.getEmail())
                    .groupName(groupName)
                    .build());
            log.info("Utilizator {} adăugat în grupul Cognito: {}", req.getEmail(), groupName);
        } catch (software.amazon.awssdk.core.exception.SdkClientException credEx) {
            log.error("[GRUP] Credențiale AWS IAM lipsă sau invalide — utilizatorul {} NU a fost adăugat în grupul {}. " +
                      "Configurează AWS_ACCESS_KEY_ID și AWS_SECRET_ACCESS_KEY cu permisiunea cognito-idp:AdminAddUserToGroup. " +
                      "Detalii: {}", req.getEmail(), groupName, credEx.getMessage());
        } catch (Exception e) {
            log.error("[GRUP] adminAddUserToGroup eșuat pentru {} în grupul {}: {}", req.getEmail(), groupName, e.getMessage());
        }

        log.info("Utilizator înregistrat: {} (sub={})", req.getEmail(), sub);
    }

    // ── VERIFY EMAIL ──────────────────────────────────────────────────────────

    public void verifyEmail(VerifyEmailRequest req) {
        ConfirmSignUpRequest.Builder builder = ConfirmSignUpRequest.builder()
                .clientId(cognitoProperties.getClientId())
                .secretHash(secretHash(req.getEmail()))
                .username(req.getEmail())
                .confirmationCode(req.getCode());
        cognitoClient.confirmSignUp(builder.build());
        log.info("Email verificat: {}", req.getEmail());
    }

    // ── RESEND VERIFICATION CODE ──────────────────────────────────────────────

    public void resendVerificationCode(ResendVerificationRequest req) {
        ResendConfirmationCodeRequest.Builder builder = ResendConfirmationCodeRequest.builder()
                .clientId(cognitoProperties.getClientId())
                .secretHash(secretHash(req.getEmail()))
                .username(req.getEmail());
        cognitoClient.resendConfirmationCode(builder.build());
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest req) {
        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("USERNAME", req.getEmail());
        params.put("PASSWORD", req.getPassword());
        String sh = secretHash(req.getEmail());
        if (sh != null) params.put("SECRET_HASH", sh);

        AuthenticationResultType tokens;
        try {
            tokens = cognitoClient.initiateAuth(
                    InitiateAuthRequest.builder()
                            .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                            .clientId(cognitoProperties.getClientId())
                            .authParameters(params)
                            .build()
            ).authenticationResult();
        } catch (InvalidParameterException e) {
            log.error("Login eșuat — USER_PASSWORD_AUTH nu este activat în App Client sau " +
                      "SECRET_HASH incorect. Verifică setările Cognito. Mesaj: {}", e.getMessage());
            throw e;
        } catch (NotAuthorizedException e) {
            log.warn("Login eșuat — credențiale invalide sau SECRET_HASH greșit pentru: {}", req.getEmail());
            throw e;
        }

        Map<String, String> attrs = cognitoClient.getUser(
                        GetUserRequest.builder().accessToken(tokens.accessToken()).build()
                ).userAttributes().stream()
                .collect(Collectors.toMap(AttributeType::name, AttributeType::value));

        String sub            = attrs.get("sub");
        boolean emailVerified = Boolean.parseBoolean(attrs.get("email_verified"));

        User user = findAndStampLogin(sub);
        log.info("Login reușit: {} (mongoId={})", req.getEmail(), user.getId());

        return AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokens.expiresIn())
                .user(toUserResponse(user, emailVerified))
                .build();
    }

    // ── REFRESH ───────────────────────────────────────────────────────────────

    public AuthResponse refresh(RefreshRequest req) {
        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("REFRESH_TOKEN", req.getRefreshToken());
        String sh = secretHash(req.getEmail());
        if (sh != null) params.put("SECRET_HASH", sh);

        AuthenticationResultType tokens = cognitoClient.initiateAuth(
                InitiateAuthRequest.builder()
                        .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                        .clientId(cognitoProperties.getClientId())
                        .authParameters(params)
                        .build()
        ).authenticationResult();

        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(req.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(tokens.expiresIn())
                .user(toUserResponse(user, true))
                .build();
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────────────────

    public void forgotPassword(ro.fiismart.auth.dto.request.ForgotPasswordRequest req) {
        software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest.Builder builder =
                software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest.builder()
                        .clientId(cognitoProperties.getClientId())
                        .secretHash(secretHash(req.getEmail()))
                        .username(req.getEmail());
        cognitoClient.forgotPassword(builder.build());
    }

    // ── RESET PASSWORD ────────────────────────────────────────────────────────

    public void resetPassword(ResetPasswordRequest req) {
        ConfirmForgotPasswordRequest.Builder builder = ConfirmForgotPasswordRequest.builder()
                .clientId(cognitoProperties.getClientId())
                .secretHash(secretHash(req.getEmail()))
                .username(req.getEmail())
                .confirmationCode(req.getCode())
                .password(req.getNewPassword());
        cognitoClient.confirmForgotPassword(builder.build());
        log.info("Parolă resetată pentru: {}", req.getEmail());
    }

    // ── OAUTH2 CODE EXCHANGE (federated IdP callback) ─────────────────────────
    //
    // The FE redirects to the Cognito Hosted UI for Google sign-in. After
    // success Cognito sends the user back to /auth/callback?code=… and the FE
    // needs to swap that code for tokens. Because our App Client has a
    // client_secret, the swap must include it — and a secret in a SPA bundle
    // is effectively public. We proxy the call through the BE instead.

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient oauthHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OAuthExchangeResponse exchangeOAuthCode(OAuthExchangeRequest req) {
        String domain = cognitoProperties.getHostedUiDomain();
        if (domain == null || domain.isBlank()) {
            log.error("aws.cognito.hosted-ui-domain is not configured — cannot exchange OAuth code");
            throw new IllegalStateException("Cognito Hosted UI domain not configured");
        }

        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", cognitoProperties.getClientId());
        form.put("redirect_uri", req.getRedirectUri());
        form.put("code", req.getCode());
        form.put("code_verifier", req.getCodeVerifier());
        String secret = cognitoProperties.getClientSecret();
        if (secret != null && !secret.isBlank()) {
            form.put("client_secret", secret);
        }

        String body = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + domain + "/oauth2/token"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = oauthHttpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Cognito token exchange failed: {}", e.getMessage());
            throw new RuntimeException("Cognito token exchange failed", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String safeBody = response.body() == null ? "" :
                    (response.body().length() > 500 ? response.body().substring(0, 500) : response.body());
            log.warn("Cognito token exchange returned {}: {}", response.statusCode(), safeBody);
            // Pass through Cognito's error to the FE — it's already an OAuth-shaped JSON
            // (e.g. {"error":"invalid_grant"}) which the FE knows how to handle.
            throw new OAuthExchangeException(response.statusCode(), response.body());
        }

        try {
            JsonNode json = objectMapper.readTree(response.body());
            return new OAuthExchangeResponse(
                    json.path("access_token").asText(null),
                    json.path("id_token").asText(null),
                    json.path("refresh_token").asText(null),
                    json.path("expires_in").asLong(3600)
            );
        } catch (Exception e) {
            log.error("Could not parse Cognito token response: {}", e.getMessage());
            throw new RuntimeException("Could not parse Cognito token response", e);
        }
    }

    public static class OAuthExchangeException extends RuntimeException {
        private final int status;
        private final String body;
        public OAuthExchangeException(int status, String body) {
            super("Cognito OAuth exchange failed: " + status);
            this.status = status;
            this.body = body;
        }
        public int getStatus() { return status; }
        public String getBody() { return body; }
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────────

    public void logout(String accessToken) {
        cognitoClient.globalSignOut(
                GlobalSignOutRequest.builder()
                        .accessToken(accessToken)
                        .build()
        );
    }

    // ── GET ME ────────────────────────────────────────────────────────────────

    public UserResponse getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // Dacă utilizatorul are cognitoSub, autentificarea Cognito a reușit → emailul e verificat.
        // Utilizatorii federați (Google) sunt întotdeauna verificați de provider.
        boolean emailVerified = user.getCognitoSub() != null;
        return toUserResponse(user, emailVerified);
    }

    public UserResponse updateMe(String userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String displayName = clean(req.getDisplayName());
        if (displayName == null) {
            String firstName = clean(req.getFirstName());
            String lastName = clean(req.getLastName());
            if (firstName != null || lastName != null) {
                displayName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            }
        }
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName);
        }
        user.setPhone(clean(req.getPhone()));
        user.setBio(clean(req.getBio()));
        user.setAvatarUrl(clean(req.getAvatarUrl()));

        User saved = userRepository.save(user);
        return toUserResponse(saved, saved.getCognitoSub() != null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findAndStampLogin(String cognitoSub) {
        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found for this account"));
        user.setLastLoginAt(new Date());
        return userRepository.save(user);
    }

    // ── ASSIGN ROLE (Google / federated users) ───────────────────────────────

    public UserResponse assignRole(String userId, String role, String firstName, String lastName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isNeedsRoleSelection()) {
            throw new IllegalStateException("Rolul a fost deja atribuit pentru acest utilizator.");
        }

        if (firstName != null && !firstName.isBlank()) {
            String fn = firstName.trim();
            String ln = (lastName != null && !lastName.isBlank()) ? " " + lastName.trim() : "";
            user.setDisplayName(fn + ln);
        }

        String normalizedRole = role.equalsIgnoreCase("PROFESSOR") ? "professor" : "student";
        String groupName      = normalizedRole.toUpperCase();

        user.setRole(normalizedRole);
        user.setNeedsRoleSelection(false);
        userRepository.save(user);
        log.info("[assign-role] {} → rol={}", user.getEmail(), normalizedRole);

        // Username-ul Cognito este necesar pentru adminAddUserToGroup.
        // Pentru utilizatori federați acesta este "Google_<sub>"; pentru nativi este email-ul.
        String cognitoUsernameForGroup = user.getCognitoUsername() != null
                ? user.getCognitoUsername()
                : user.getEmail();

        try {
            cognitoClient.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(cognitoUsernameForGroup)
                    .groupName(groupName)
                    .build());
            log.info("[assign-role] {} adăugat în grupul Cognito: {}", cognitoUsernameForGroup, groupName);
        } catch (Exception e) {
            log.error("[assign-role] Eroare la adăugarea în grupul Cognito pentru {}: {}",
                    cognitoUsernameForGroup, e.getMessage());
        }

        return toUserResponse(user, true);
    }

    public UserResponse toUserResponse(User user, boolean emailVerified) {
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "";
        int spaceIdx = displayName.indexOf(' ');
        String firstName = spaceIdx >= 0 ? displayName.substring(0, spaceIdx) : displayName;
        String lastName  = spaceIdx >= 0 ? displayName.substring(spaceIdx + 1) : "";

        String rawRole = user.getRole() != null ? user.getRole().toLowerCase() : "student";
        String normalizedRole;
        if (rawRole.equals("admin")) {
            normalizedRole = "ADMIN";
        } else if (rawRole.equals("professor") || rawRole.equals("teacher")) {
            normalizedRole = "PROFESSOR";
        } else {
            normalizedRole = "STUDENT";
        }

        return UserResponse.builder()
                .id(user.getId())
                .cognitoSub(user.getCognitoSub())
                .email(user.getEmail())
                .firstName(firstName)
                .lastName(lastName)
                .displayName(displayName)
                .role(normalizedRole)
                .phone(user.getPhone())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(emailVerified)
                .needsRoleSelection(user.isNeedsRoleSelection())
                .banned(user.isBanned())
                .bannedBy(user.getBannedBy())
                .bannedAt(user.getBannedAt())
                .banReason(user.getBanReason())
                .ownedCourses(user.getOwnedCourses())
                .enrolledCourseIds(user.getEnrolledCourseIds())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
