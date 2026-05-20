package ro.fiismart.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleUserNotConfirmed_returns403() {
        UserNotConfirmedException ex = UserNotConfirmedException.builder()
                .message("not confirmed").build();

        ResponseEntity<?> response = handler.handleUserNotConfirmed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().toString()).contains("USER_NOT_CONFIRMED");
    }

    @Test
    void handleNotAuthorized_returns401() {
        NotAuthorizedException ex = NotAuthorizedException.builder()
                .message("wrong password").build();

        ResponseEntity<?> response = handler.handleNotAuthorized(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleUserExists_returns409() {
        UsernameExistsException ex = UsernameExistsException.builder()
                .message("exists").build();

        ResponseEntity<?> response = handler.handleUserExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleAliasExists_returns409() {
        AliasExistsException ex = AliasExistsException.builder()
                .message("alias exists").build();

        ResponseEntity<?> response = handler.handleAliasExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleCognitoUserNotFound_returns404() {
        UserNotFoundException ex = UserNotFoundException.builder()
                .message("not found").build();

        ResponseEntity<?> response = handler.handleCognitoUserNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleCodeMismatch_returns400() {
        CodeMismatchException ex = CodeMismatchException.builder()
                .message("code mismatch").build();

        ResponseEntity<?> response = handler.handleCodeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleExpiredCode_returns400() {
        ExpiredCodeException ex = ExpiredCodeException.builder()
                .message("expired").build();

        ResponseEntity<?> response = handler.handleExpiredCode(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleTooManyRequests_returns429() {
        TooManyRequestsException ex = TooManyRequestsException.builder()
                .message("too many").build();

        ResponseEntity<?> response = handler.handleTooManyRequests(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(429);
    }

    @Test
    void handleInvalidPassword_returns400WithPasswordField() {
        InvalidPasswordException ex = InvalidPasswordException.builder()
                .message("weak password").build();

        ResponseEntity<?> response = handler.handleInvalidPassword(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleInvalidParameter_returns400() {
        InvalidParameterException ex = InvalidParameterException.builder()
                .message("invalid param").build();

        ResponseEntity<?> response = handler.handleInvalidParameter(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleLimitExceeded_returns429() {
        LimitExceededException ex = LimitExceededException.builder()
                .message("limit exceeded").build();

        ResponseEntity<?> response = handler.handleLimitExceeded(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(429);
    }

    @Test
    void handleNotFound_returns404WithMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Course", "123");

        ResponseEntity<Map<String, String>> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("error");
        assertThat(response.getBody().get("error")).contains("Course");
    }

    @Test
    void handleValidation_returns400WithErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "email", "must not be blank");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    void handleIllegalArgument_returns400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad arg"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).isEqualTo("bad arg");
    }

    @Test
    void handleIllegalState_returns409() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalState(new IllegalStateException("bad state"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleGeneric_returns500() {
        ResponseEntity<Map<String, String>> response =
                handler.handleGeneric(new RuntimeException("unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
