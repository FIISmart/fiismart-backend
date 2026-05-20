package ro.fiismart.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.fiismart.auth.dto.UserRole;
import ro.fiismart.auth.dto.request.*;
import ro.fiismart.auth.dto.response.AuthResponse;
import ro.fiismart.auth.dto.response.UserResponse;
import ro.fiismart.auth.service.CognitoAuthService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private CognitoAuthService authService;
    @InjectMocks private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    private RegisterRequest validRegisterReq() {
        return new RegisterRequest("Ion", "Popescu", "ion@test.com", "Parola1!", UserRole.STUDENT);
    }

    @Test
    void register_returnsCreated() throws Exception {
        doNothing().when(authService).register(any());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validRegisterReq())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void signup_returnsCreated() throws Exception {
        doNothing().when(authService).register(any());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validRegisterReq())))
                .andExpect(status().isCreated());
    }

    @Test
    void verifyEmail_returnsOk() throws Exception {
        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("ion@test.com");
        req.setCode("123456");
        doNothing().when(authService).verifyEmail(any());

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void resendVerification_returnsOk() throws Exception {
        ResendVerificationRequest req = new ResendVerificationRequest();
        req.setEmail("ion@test.com");
        doNothing().when(authService).resendVerificationCode(any());

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void login_returnsOk() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("ion@test.com");
        req.setPassword("Parola1!");
        AuthResponse resp = AuthResponse.builder().accessToken("tok123").tokenType("Bearer").build();
        when(authService.login(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("tok123"));
    }

    @Test
    void refresh_returnsOk() throws Exception {
        RefreshRequest req = new RefreshRequest();
        req.setEmail("ion@test.com");
        req.setRefreshToken("refresh123");
        AuthResponse resp = AuthResponse.builder().accessToken("newTok").tokenType("Bearer").build();
        when(authService.refresh(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_returnsOk() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("ion@test.com");
        doNothing().when(authService).forgotPassword(any());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_returnsOk() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("ion@test.com");
        req.setCode("123456");
        req.setNewPassword("Parola2!");
        doNothing().when(authService).resetPassword(any());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void logout_withBearerToken_returnsNoContent() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_missingToken_throwsException() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/auth/logout")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void me_returnsOk() throws Exception {
        UserResponse resp = UserResponse.builder().id("u1").email("ion@test.com").build();
        when(authService.getMe(any())).thenReturn(resp);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk());
    }

    @Test
    void assignRole_returnsOk() throws Exception {
        AssignRoleRequest req = new AssignRoleRequest();
        req.setRole("STUDENT");
        req.setFirstName("Ion");
        req.setLastName("Popescu");
        UserResponse resp = UserResponse.builder().id("u1").role("STUDENT").build();
        when(authService.assignRole(any(), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
