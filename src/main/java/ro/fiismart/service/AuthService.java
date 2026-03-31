package ro.fiismart.service;

import database.dao.UserDAO;
import database.model.User;
import org.bson.types.ObjectId;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import ro.fiismart.dto.*;

import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuthService — the bridge layer.
 *
 * This is where the two worlds meet:
 *   - LEFT SIDE:   MongoDB DAO (UserDAO, User model, ObjectIds, lowercase roles, displayName)
 *   - RIGHT SIDE:  frontend contract (firstName+lastName, uppercase roles, string IDs, tokens)
 *
 * Every method in this class does two things:
 *   1. Calls the appropriate DAO method to read/write MongoDB
 *   2. Translates between the two different data shapes
 */
@Service
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    // ── In-memory token store ────────────────────────────────────────────────
    // Maps accessToken → userId (hex string)
    // Maps refreshToken → userId (hex string)
    // Maps resetToken → userId (hex string)
    //
    // NOTE: When the team is ready to upgrade to real JWTs, replace these
    // three maps with a proper JWT library. Nothing else in this file changes.
    private final Map<String, String> accessTokens  = new ConcurrentHashMap<>();
    private final Map<String, String> refreshTokens = new ConcurrentHashMap<>();
    private final Map<String, String> resetTokens   = new ConcurrentHashMap<>();
    private final Map<String, String> verifyTokens  = new ConcurrentHashMap<>();

    // ── LOGIN ────────────────────────────────────────────────────────────────

    public ApiResponse<AuthResponse> login(LoginRequest req) {
        if (req.email() == null || req.password() == null) {
            return ApiResponse.error("Email and password are required.");
        }

        // 1. Find user in MongoDB by email
        User user = userDAO.findByEmail(req.email().toLowerCase().trim());

        // 2. Check user exists AND password matches the BCrypt hash
        //    BCrypt.checkpw handles the hash comparison safely
        if (user == null || !BCrypt.checkpw(req.password(), user.getPasswordHash())) {
            return ApiResponse.error("Invalid email or password.");
        }

        if (user.isBanned()) {
            return ApiResponse.error("Your account has been suspended. Reason: " + user.getBanReason());
        }

        // 3. Update lastLoginAt in MongoDB
        userDAO.updateLastLogin(user.getId(), new Date());

        // 4. Generate tokens and store them
        String accessToken  = generateToken();
        String refreshToken = generateToken();
        accessTokens.put(accessToken, user.getId().toHexString());
        refreshTokens.put(refreshToken, user.getId().toHexString());

        return ApiResponse.ok(AuthResponse.of(accessToken, refreshToken, toDto(user)));
    }

    // ── REGISTER ─────────────────────────────────────────────────────────────

    public ApiResponse<AuthResponse> register(RegisterRequest req) {
        if (req.email() == null || req.password() == null ||
            req.firstName() == null || req.lastName() == null) {
            return ApiResponse.error("All fields are required.");
        }

        String email = req.email().toLowerCase().trim();

        // 1. Check if email is already taken
        if (userDAO.existsByEmail(email)) {
            return ApiResponse.fieldError("email", "Email is already in use.");
        }

        if (req.password().length() < 8) {
            return ApiResponse.fieldError("password", "Password must be at least 8 characters.");
        }

        // 2. Bridge: frontend sends "STUDENT" / "TEACHER", DB stores "student" / "teacher"
        String dbRole = req.role() != null ? req.role().toLowerCase() : "student";

        // 3. Bridge: frontend sends firstName + lastName, DB stores a single displayName
        String displayName = req.firstName().trim() + " " + req.lastName().trim();

        // 4. Hash the password with BCrypt before storing — NEVER store plain text
        String passwordHash = BCrypt.hashpw(req.password(), BCrypt.gensalt());

        // 5. Build and insert the User using their builder pattern
        User newUser = User.builder()
                .displayName(displayName)
                .email(email)
                .passwordHash(passwordHash)
                .role(dbRole)
                .createdAt(new Date())
                .banned(false)
                .build();

        ObjectId newId = userDAO.insert(newUser);
        newUser = userDAO.findById(newId); // re-fetch so we have the full persisted object

        // 6. Generate tokens
        String accessToken  = generateToken();
        String refreshToken = generateToken();
        accessTokens.put(accessToken, newId.toHexString());
        refreshTokens.put(refreshToken, newId.toHexString());

        return ApiResponse.ok(
                AuthResponse.of(accessToken, refreshToken, toDto(newUser),
                        "Registration successful! Please verify your email."));
    }

    // ── LOGOUT ───────────────────────────────────────────────────────────────

    public ApiResponse<Void> logout(String bearerHeader) {
        String token = extractBearer(bearerHeader);
        if (token != null) {
            String userId = accessTokens.remove(token);
            if (userId != null) {
                // Also invalidate all refresh tokens for this user
                refreshTokens.values().removeIf(id -> id.equals(userId));
            }
        }
        return ApiResponse.ok(null, "Logged out successfully.");
    }

    // ── REFRESH TOKEN ─────────────────────────────────────────────────────────

    public ApiResponse<AuthResponse> refresh(String refreshToken) {
        String userId = refreshTokens.get(refreshToken);
        if (userId == null) {
            return ApiResponse.error("Invalid or expired refresh token.");
        }

        User user = userDAO.findById(new ObjectId(userId));
        if (user == null) return ApiResponse.error("User not found.");

        // Rotate both tokens (security best practice)
        refreshTokens.remove(refreshToken);
        String newAccess  = generateToken();
        String newRefresh = generateToken();
        accessTokens.put(newAccess, userId);
        refreshTokens.put(newRefresh, userId);

        return ApiResponse.ok(AuthResponse.of(newAccess, newRefresh, toDto(user)));
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────────────────

    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest req) {
        User user = userDAO.findByEmail(req.email().toLowerCase().trim());

        if (user != null) {
            String resetToken = generateToken();
            resetTokens.put(resetToken, user.getId().toHexString());

            // In production: send this token via email
            // For now, print it so you can test manually
            System.out.printf("""
                    [AUTH] Password reset token for %s:
                           Token: %s
                           URL:   http://localhost:5173/reset-password?token=%s%n""",
                    user.getEmail(), resetToken, resetToken);
        }

        return ApiResponse.ok(null, "If that email is registered, a reset link has been sent.");
    }

    // ── RESET PASSWORD ────────────────────────────────────────────────────────

    public ApiResponse<Void> resetPassword(ResetPasswordRequest req) {
        String userId = resetTokens.get(req.token());
        if (userId == null) return ApiResponse.error("Invalid or expired reset token.");

        if (!req.newPassword().equals(req.confirmPassword())) {
            return ApiResponse.fieldError("confirmPassword", "Passwords do not match.");
        }

        if (req.newPassword().length() < 8) {
            return ApiResponse.fieldError("newPassword", "Password must be at least 8 characters.");
        }

        // Hash the new password before storing
        String newHash = BCrypt.hashpw(req.newPassword(), BCrypt.gensalt());
        userDAO.updatePasswordHash(new ObjectId(userId), newHash);
        resetTokens.remove(req.token());

        // Invalidate all existing sessions for security
        accessTokens.values().removeIf(id -> id.equals(userId));
        refreshTokens.values().removeIf(id -> id.equals(userId));

        return ApiResponse.ok(null, "Password reset successfully. You can now log in.");
    }

    // ── VERIFY EMAIL ──────────────────────────────────────────────────────────

    public ApiResponse<Void> verifyEmail(String token) {
        String userId = verifyTokens.get(token);
        if (userId == null) return ApiResponse.error("Invalid or expired verification token.");

        verifyTokens.remove(token);

        return ApiResponse.ok(null, "Email verified successfully.");
    }

    // ── GET CURRENT USER (/me) ────────────────────────────────────────────────

    public ApiResponse<UserDto> getCurrentUser(String bearerHeader) {
        String token = extractBearer(bearerHeader);
        if (token == null) return ApiResponse.error("Missing Authorization header.");

        String userId = accessTokens.get(token);
        if (userId == null) return ApiResponse.error("Token is invalid or has expired.");

        User user = userDAO.findById(new ObjectId(userId));
        if (user == null) return ApiResponse.error("User not found.");

        return ApiResponse.ok(toDto(user));
    }

    // ── BRIDGE: DB User → Frontend UserDto ───────────────────────────────────

    /**
     * This method is the heart of the bridging.
     * It converts the DB-side User model into the shape your frontend reads.
     */
    private UserDto toDto(User user) {
        // Bridge 1: split displayName "Ana Ionescu" → firstName="Ana", lastName="Ionescu"
        // If there's no space (e.g. "Admin"), firstName = whole name, lastName = ""
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "";
        int spaceIdx = displayName.indexOf(' ');
        String firstName = spaceIdx >= 0 ? displayName.substring(0, spaceIdx) : displayName;
        String lastName  = spaceIdx >= 0 ? displayName.substring(spaceIdx + 1) : "";

        // Bridge 2: lowercase DB role → uppercase frontend role
        // "student" → "STUDENT", "teacher" → "TEACHER", "admin" → "ADMIN"
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "STUDENT";

        // Bridge 3: ObjectId → plain String
        String id = user.getId() != null ? user.getId().toHexString() : null;

        // Bridge 4: emailVerified — field doesn't exist in DB yet, default false
        boolean emailVerified = false;

        // Bridge 5: Date → ISO string
        String createdAt = user.getCreatedAt() != null
                ? user.getCreatedAt().toInstant().toString()
                : null;

        return new UserDto(id, user.getEmail(), firstName, lastName,
                role, emailVerified, createdAt);
    }

    // ── INTERNAL HELPERS ──────────────────────────────────────────────────────

    private String generateToken() {
        String raw = UUID.randomUUID().toString().replace("-", "")
                   + UUID.randomUUID().toString().replace("-", "");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    private String extractBearer(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
