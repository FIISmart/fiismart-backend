package ro.fiismart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.dto.*;
import ro.fiismart.service.AuthService;

/**
 * Exposes the exact 8 endpoints your frontend's auth.service.ts calls.
 * Each method does almost nothing — it receives the request, calls the
 * service, picks the right HTTP status code, and returns.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest req) {
        ApiResponse<AuthResponse> res = authService.login(req);
        return ResponseEntity.status(res.success() ? 200 : 401).body(res);
    }

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest req) {
        ApiResponse<AuthResponse> res = authService.register(req);
        return ResponseEntity.status(res.success() ? 201 : 400).body(res);
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.logout(authHeader));
    }

    // POST /api/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String refreshToken = body != null ? body.get("refreshToken") : null;
        if (refreshToken == null) {
            return ResponseEntity.status(400).body(ApiResponse.error("refreshToken is required."));
        }
        ApiResponse<AuthResponse> res = authService.refresh(refreshToken);
        return ResponseEntity.status(res.success() ? 200 : 401).body(res);
    }

    // POST /api/auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(authService.forgotPassword(req));
    }

    // POST /api/auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest req) {
        ApiResponse<Void> res = authService.resetPassword(req);
        return ResponseEntity.status(res.success() ? 200 : 400).body(res);
    }

    // POST /api/auth/verify-email
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody java.util.Map<String, String> body) {
        String token = body != null ? body.get("token") : null;
        if (token == null) {
            return ResponseEntity.status(400).body(ApiResponse.error("token is required."));
        }
        ApiResponse<Void> res = authService.verifyEmail(token);
        return ResponseEntity.status(res.success() ? 200 : 400).body(res);
    }

    // GET /api/auth/me
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ApiResponse<UserDto> res = authService.getCurrentUser(authHeader);
        return ResponseEntity.status(res.success() ? 200 : 401).body(res);
    }
}
