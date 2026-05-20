package ro.fiismart.auth.dto;

import org.junit.jupiter.api.Test;
import ro.fiismart.auth.dto.request.*;
import ro.fiismart.auth.dto.response.AuthResponse;
import ro.fiismart.auth.dto.response.UserResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDTOTest {

    @Test
    void registerRequest_gettersSetters() {
        RegisterRequest req = new RegisterRequest("Ion", "Pop", "ion@x.com", "Parola1!", UserRole.PROFESSOR);
        assertThat(req.getFirstName()).isEqualTo("Ion");
        assertThat(req.getLastName()).isEqualTo("Pop");
        assertThat(req.getEmail()).isEqualTo("ion@x.com");
        assertThat(req.getRole()).isEqualTo(UserRole.PROFESSOR);
    }

    @Test
    void loginRequest_gettersSetters() {
        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com");
        req.setPassword("pass");
        assertThat(req.getEmail()).isEqualTo("a@b.com");
        assertThat(req.getPassword()).isEqualTo("pass");
    }

    @Test
    void verifyEmailRequest_gettersSetters() {
        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("v@v.com");
        req.setCode("123456");
        assertThat(req.getEmail()).isEqualTo("v@v.com");
        assertThat(req.getCode()).isEqualTo("123456");
    }

    @Test
    void resendVerificationRequest_gettersSetters() {
        ResendVerificationRequest req = new ResendVerificationRequest();
        req.setEmail("r@r.com");
        assertThat(req.getEmail()).isEqualTo("r@r.com");
    }

    @Test
    void refreshRequest_gettersSetters() {
        RefreshRequest req = new RefreshRequest();
        req.setEmail("x@x.com");
        req.setRefreshToken("tok");
        assertThat(req.getEmail()).isEqualTo("x@x.com");
        assertThat(req.getRefreshToken()).isEqualTo("tok");
    }

    @Test
    void forgotPasswordRequest_gettersSetters() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("f@f.com");
        assertThat(req.getEmail()).isEqualTo("f@f.com");
    }

    @Test
    void resetPasswordRequest_gettersSetters() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("rp@rp.com");
        req.setCode("654321");
        req.setNewPassword("NewPass1!");
        assertThat(req.getEmail()).isEqualTo("rp@rp.com");
        assertThat(req.getCode()).isEqualTo("654321");
        assertThat(req.getNewPassword()).isEqualTo("NewPass1!");
    }

    @Test
    void assignRoleRequest_gettersSetters() {
        AssignRoleRequest req = new AssignRoleRequest();
        req.setRole("STUDENT");
        req.setFirstName("Ion");
        req.setLastName("Pop");
        assertThat(req.getRole()).isEqualTo("STUDENT");
        assertThat(req.getFirstName()).isEqualTo("Ion");
        assertThat(req.getLastName()).isEqualTo("Pop");
    }

    @Test
    void userRole_values() {
        assertThat(UserRole.values()).contains(UserRole.STUDENT, UserRole.PROFESSOR);
        assertThat(UserRole.valueOf("STUDENT")).isEqualTo(UserRole.STUDENT);
    }

    @Test
    void authResponse_builderAndGetters() {
        AuthResponse resp = AuthResponse.builder()
                .accessToken("acc")
                .refreshToken("ref")
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();
        assertThat(resp.getAccessToken()).isEqualTo("acc");
        assertThat(resp.getRefreshToken()).isEqualTo("ref");
        assertThat(resp.getTokenType()).isEqualTo("Bearer");
        assertThat(resp.getExpiresIn()).isEqualTo(3600);
    }

    @Test
    void userResponse_builderAndGetters() {
        UserResponse resp = UserResponse.builder()
                .id("u1")
                .email("a@b.com")
                .firstName("Ion")
                .lastName("Pop")
                .role("STUDENT")
                .emailVerified(true)
                .needsRoleSelection(false)
                .build();
        assertThat(resp.getId()).isEqualTo("u1");
        assertThat(resp.getFirstName()).isEqualTo("Ion");
        assertThat(resp.isEmailVerified()).isTrue();
        assertThat(resp.isNeedsRoleSelection()).isFalse();
    }
}
