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
import ro.fiismart.auth.dto.response.UserResponse;
import ro.fiismart.auth.service.CognitoAdminService;
import ro.fiismart.auth.service.CognitoAuthService;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CognitoAuthControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private CognitoAuthService cognitoAuthService;
    @Mock private CognitoAdminService cognitoAdminService;
    @InjectMocks private CognitoAuthController cognitoAuthController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cognitoAuthController).build();
    }

    @Test
    void getCurrentUser_nullUserId_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cognito/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTokenInfo_nullUserId_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cognito/token-info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser_missingEmail_returnsBadRequest() throws Exception {
        Map<String, String> body = Map.of("temporaryPassword", "Pass1!");

        mockMvc.perform(post("/api/v1/cognito/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_validRequest_returnsCreated() throws Exception {
        Map<String, String> body = Map.of("email", "new@test.com", "temporaryPassword", "Pass1!", "role", "STUDENT");
        doNothing().when(cognitoAdminService).createUser(any(), any(), any());

        mockMvc.perform(post("/api/v1/cognito/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteUser_callsService() throws Exception {
        doNothing().when(cognitoAdminService).deleteUser(any());

        mockMvc.perform(delete("/api/v1/cognito/admin/users/test@test.com"))
                .andExpect(status().isOk());

        verify(cognitoAdminService).deleteUser("test@test.com");
    }
}
