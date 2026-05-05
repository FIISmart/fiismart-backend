package ro.fiismart.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.auth.dto.response.UserResponse;
import ro.fiismart.auth.service.CognitoAdminService;
import ro.fiismart.auth.service.CognitoAuthService;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cognito")
@RequiredArgsConstructor
public class CognitoAuthController {

    private final UserRepository userRepository;
    private final CognitoAuthService cognitoAuthService;
    private final CognitoAdminService cognitoAdminService;

    /**
     * GET /api/v1/cognito/me
     * Returnează profilul utilizatorului autentificat.
     * Principal = MongoDB userId (setat de CognitoJwtAuthenticationConverter).
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal String userId,
                                            HttpServletRequest request) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        UserResponse response = cognitoAuthService.toUserResponse(user, true);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/cognito/token-info — info debug din SecurityContext
     */
    @GetMapping("/token-info")
    public ResponseEntity<?> getTokenInfo(@AuthenticationPrincipal String userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        return ResponseEntity.ok(Map.of("userId", userId));
    }

    /**
     * POST /api/v1/cognito/admin/users — creare utilizator (doar profesori)
     */
    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String email             = body.get("email");
        String temporaryPassword = body.get("temporaryPassword");
        String role              = body.getOrDefault("role", "STUDENT");

        if (email == null || temporaryPassword == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Câmpurile 'email' și 'temporaryPassword' sunt obligatorii."));
        }
        cognitoAdminService.createUser(email, temporaryPassword, role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Utilizator creat cu rolul " + role.toUpperCase() + "."));
    }

    /**
     * DELETE /api/v1/cognito/admin/users/{email} — ștergere utilizator (doar profesori)
     */
    @DeleteMapping("/admin/users/{email}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        cognitoAdminService.deleteUser(email);
        return ResponseEntity.ok(Map.of("message", "Utilizatorul a fost șters din Cognito."));
    }
}
