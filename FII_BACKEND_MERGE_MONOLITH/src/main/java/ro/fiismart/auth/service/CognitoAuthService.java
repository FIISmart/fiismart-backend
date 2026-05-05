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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
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
        log.info("  Has Secret:   {}", hasClientSecret());
        log.info("  JWKS URI:     {}", cognitoProperties.getJwksUri());
        if (!hasClientSecret()) {
            log.warn("  [!] AWS_COGNITO_CLIENT_SECRET nu este setat. " +
                     "Dacă App Client-ul are secret configurat, autentificarea va eșua.");
        }
    }

    // ── SECRET HASH ───────────────────────────────────────────────────────────

    private boolean hasClientSecret() {
        String s = cognitoProperties.getClientSecret();
        return s != null && !s.isBlank();
    }

    /**
     * Calculează SECRET_HASH = Base64(HMAC-SHA256(email + clientId)).
     * În acest User Pool, Cognito username = adresa de email a utilizatorului.
     */
    private String computeSecretHash(String email) {
        try {
            String message = email + cognitoProperties.getClientId();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    cognitoProperties.getClientSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            ));
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute Cognito SECRET_HASH", e);
        }
    }

    /** Adaugă SECRET_HASH în params — email-ul este întotdeauna Cognito username în acest pool. */
    private Map<String, String> authParams(String email, Map<String, String> base) {
        Map<String, String> params = new HashMap<>(base);
        if (hasClientSecret()) {
            params.put("SECRET_HASH", computeSecretHash(email));
        }
        return params;
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    public void register(RegisterRequest req) {
        SignUpRequest.Builder builder = SignUpRequest.builder()
                .clientId(cognitoProperties.getClientId())
                .username(req.getEmail())
                .password(req.getPassword())
                .userAttributes(
                        AttributeType.builder().name("email").value(req.getEmail()).build(),
                        AttributeType.builder().name("given_name").value(req.getFirstName()).build(),
                        AttributeType.builder().name("family_name").value(req.getLastName()).build(),
                        AttributeType.builder().name("name").value(req.getFirstName() + " " + req.getLastName()).build()
                );
        if (hasClientSecret()) {
            builder.secretHash(computeSecretHash(req.getEmail()));
        }

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
            log.debug("adminAddUserToGroup ignorat — credențiale AWS lipsă în mediul local: {}", credEx.getMessage());
        } catch (Exception e) {
            log.warn("Nu s-a putut adăuga {} în grupul {}: {}", req.getEmail(), groupName, e.getMessage());
        }

        log.info("Utilizator înregistrat: {} (sub={})", req.getEmail(), sub);
    }

    // ── VERIFY EMAIL ──────────────────────────────────────────────────────────

    public void verifyEmail(VerifyEmailRequest req) {
        ConfirmSignUpRequest.Builder builder = ConfirmSignUpRequest.builder()
                .clientId(cognitoProperties.getClientId())
                .username(req.getEmail())
                .confirmationCode(req.getCode());
        if (hasClientSecret()) {
            builder.secretHash(computeSecretHash(req.getEmail()));
        }
        cognitoClient.confirmSignUp(builder.build());
        log.info("Email verificat: {}", req.getEmail());
    }

    // ── RESEND VERIFICATION CODE ──────────────────────────────────────────────

    public void resendVerificationCode(ResendVerificationRequest req) {
        ResendConfirmationCodeRequest.Builder builder = ResendConfirmationCodeRequest.builder()
                .clientId(cognitoProperties.getClientId())
                .username(req.getEmail());
        if (hasClientSecret()) {
            builder.secretHash(computeSecretHash(req.getEmail()));
        }
        cognitoClient.resendConfirmationCode(builder.build());
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest req) {
        Map<String, String> params = authParams(req.getEmail(), Map.of(
                "USERNAME", req.getEmail(),
                "PASSWORD", req.getPassword()
        ));

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
        // SECRET_HASH = HMAC(email + clientId) — Cognito username = email în acest pool
        Map<String, String> params = authParams(req.getEmail(), Map.of(
                "REFRESH_TOKEN", req.getRefreshToken()
        ));

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
                        .username(req.getEmail());
        if (hasClientSecret()) {
            builder.secretHash(computeSecretHash(req.getEmail()));
        }
        cognitoClient.forgotPassword(builder.build());
    }

    // ── RESET PASSWORD ────────────────────────────────────────────────────────

    public void resetPassword(ResetPasswordRequest req) {
        ConfirmForgotPasswordRequest.Builder builder = ConfirmForgotPasswordRequest.builder()
                .clientId(cognitoProperties.getClientId())
                .username(req.getEmail())
                .confirmationCode(req.getCode())
                .password(req.getNewPassword());
        if (hasClientSecret()) {
            builder.secretHash(computeSecretHash(req.getEmail()));
        }
        cognitoClient.confirmForgotPassword(builder.build());
        log.info("Parolă resetată pentru: {}", req.getEmail());
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

    public UserResponse getMe(String accessToken) {
        Map<String, String> attrs = cognitoClient.getUser(
                        GetUserRequest.builder().accessToken(accessToken).build()
                ).userAttributes().stream()
                .collect(Collectors.toMap(AttributeType::name, AttributeType::value));

        String sub            = attrs.get("sub");
        boolean emailVerified = Boolean.parseBoolean(attrs.get("email_verified"));

        User user = userRepository.findByCognitoSub(sub)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return toUserResponse(user, emailVerified);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findAndStampLogin(String cognitoSub) {
        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found for this account"));
        user.setLastLoginAt(new Date());
        return userRepository.save(user);
    }

    public UserResponse toUserResponse(User user, boolean emailVerified) {
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "";
        int spaceIdx = displayName.indexOf(' ');
        String firstName = spaceIdx >= 0 ? displayName.substring(0, spaceIdx) : displayName;
        String lastName  = spaceIdx >= 0 ? displayName.substring(spaceIdx + 1) : "";

        String rawRole = user.getRole() != null ? user.getRole().toLowerCase() : "student";
        String normalizedRole = (rawRole.equals("professor") || rawRole.equals("teacher"))
                ? "PROFESSOR" : "STUDENT";

        return UserResponse.builder()
                .id(user.getId())
                .cognitoSub(user.getCognitoSub())
                .email(user.getEmail())
                .firstName(firstName)
                .lastName(lastName)
                .displayName(displayName)
                .role(normalizedRole)
                .emailVerified(emailVerified)
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
}
