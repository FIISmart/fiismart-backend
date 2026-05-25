package ro.fiismart.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.auth.dto.request.CreateUserRequest;
import ro.fiismart.auth.dto.response.UserResponse;
import ro.fiismart.auth.service.CognitoAdminService;
import ro.fiismart.auth.service.CognitoAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cognito")
@RequiredArgsConstructor
public class CognitoAuthController {

    private final CognitoAuthService cognitoAuthService;
    private final CognitoAdminService cognitoAdminService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(cognitoAuthService.getMe(userId));
    }

    @GetMapping("/token-info")
    public ResponseEntity<?> getTokenInfo(@AuthenticationPrincipal String userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        return ResponseEntity.ok(Map.of("userId", userId));
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req) {
        String role = "STUDENT";
        cognitoAdminService.createUser(req.getEmail(), req.getTemporaryPassword(), role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Utilizator creat cu rolul " + role + "."));
    }

    @DeleteMapping("/admin/users/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        cognitoAdminService.deleteUser(email);
        return ResponseEntity.ok(Map.of("message", "Utilizatorul a fost șters din Cognito."));
    }
}
