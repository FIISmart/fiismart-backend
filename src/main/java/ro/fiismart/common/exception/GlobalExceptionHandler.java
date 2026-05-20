package ro.fiismart.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ro.fiismart.ai.client.GeminiException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Excepții Cognito ──────────────────────────────────────────────────────

    @ExceptionHandler(UserNotConfirmedException.class)
    public ResponseEntity<?> handleUserNotConfirmed(UserNotConfirmedException e) {
        return ResponseEntity.status(403).body(Map.of(
                "message", "Please verify your email before signing in.",
                "code", "USER_NOT_CONFIRMED"
        ));
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<?> handleNotAuthorized(NotAuthorizedException e) {
        return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
    }

    @ExceptionHandler(UsernameExistsException.class)
    public ResponseEntity<?> handleUserExists(UsernameExistsException e) {
        return ResponseEntity.status(409).body(Map.of("message", "An account with this email already exists"));
    }

    @ExceptionHandler(AliasExistsException.class)
    public ResponseEntity<?> handleAliasExists(AliasExistsException e) {
        return ResponseEntity.status(409).body(Map.of("message", "An account with this email already exists"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleCognitoUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("message", "User not found"));
    }

    @ExceptionHandler(CodeMismatchException.class)
    public ResponseEntity<?> handleCodeMismatch(CodeMismatchException e) {
        return ResponseEntity.status(400).body(Map.of("message", "Invalid or expired verification code"));
    }

    @ExceptionHandler(ExpiredCodeException.class)
    public ResponseEntity<?> handleExpiredCode(ExpiredCodeException e) {
        return ResponseEntity.status(400).body(Map.of("message", "Verification code has expired"));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<?> handleTooManyRequests(TooManyRequestsException e) {
        return ResponseEntity.status(429).body(Map.of("message", "Too many attempts, please try again later"));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<?> handleInvalidPassword(InvalidPasswordException e) {
        return ResponseEntity.status(400).body(fieldError("password", "Password does not meet requirements"));
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<?> handleInvalidParameter(InvalidParameterException e) {
        log.error("Cognito InvalidParameterException: {}", e.getMessage());
        return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<?> handleLimitExceeded(LimitExceededException e) {
        return ResponseEntity.status(429).body(Map.of("message", "Attempt limit exceeded, please try after some time"));
    }

    // ── Excepții generale ─────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fieldError.getField(), k -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Validation failed", "errors", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<Map<String, String>> handleGemini(GeminiException ex) {
        // Do NOT log the cause's stack trace — upstream exceptions can carry the
        // request URI in their getMessage()/toString(), and Gemini's URI used to
        // include the API key as a query param. Header-based auth + cause-free
        // logging both defend against that leak path.
        log.warn("Gemini upstream error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "AI service unavailable", "code", "AI_UPSTREAM_ERROR"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "Fisierul depaseste limita de 15 MB", "code", "PDF_TOO_LARGE"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An unexpected error occurred"));
    }

    private Map<String, Object> fieldError(String field, String message) {
        return Map.of(
                "message", "Validation failed",
                "errors", Map.of(field, List.of(message))
        );
    }
}
