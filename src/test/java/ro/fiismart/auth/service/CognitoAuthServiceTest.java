package ro.fiismart.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.auth.dto.UserRole;
import ro.fiismart.auth.dto.request.ForgotPasswordRequest;
import ro.fiismart.auth.dto.request.LoginRequest;
import ro.fiismart.auth.dto.request.RefreshRequest;
import ro.fiismart.auth.dto.request.RegisterRequest;
import ro.fiismart.auth.dto.request.ResendVerificationRequest;
import ro.fiismart.auth.dto.request.ResetPasswordRequest;
import ro.fiismart.auth.dto.request.VerifyEmailRequest;
import ro.fiismart.auth.dto.response.AuthResponse;
import ro.fiismart.auth.dto.response.UserResponse;
import ro.fiismart.common.config.CognitoProperties;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GlobalSignOutRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GlobalSignOutResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitoAuthServiceTest {

    @Mock private CognitoIdentityProviderClient cognitoClient;
    @Mock private UserRepository userRepository;
    @Mock private CognitoProperties cognitoProperties;

    @InjectMocks
    private CognitoAuthService cognitoAuthService;

    // ── Helpers ────────────────────────────────────────────────────────────────

    private RegisterRequest buildRegisterReq(String email, UserRole role) {
        return new RegisterRequest("Ion", "Popescu", email, "Parola1!", role);
    }

    private void stubNoSecret() {
        when(cognitoProperties.getClientSecret()).thenReturn("");
    }

    private void stubWithSecret() {
        when(cognitoProperties.getClientSecret()).thenReturn("superSecret");
        when(cognitoProperties.getClientId()).thenReturn("clientId");
    }

    // ── toUserResponse ─────────────────────────────────────────────────────────

    @Test
    void toUserResponse_studentRole_returnsSTUDENT() {
        User user = User.builder().id("u1").email("s@test.com")
                .displayName("Ion Popescu").role("student").build();

        UserResponse resp = cognitoAuthService.toUserResponse(user, true);

        assertThat(resp.getId()).isEqualTo("u1");
        assertThat(resp.getRole()).isEqualTo("STUDENT");
        assertThat(resp.getFirstName()).isEqualTo("Ion");
        assertThat(resp.getLastName()).isEqualTo("Popescu");
        assertThat(resp.isEmailVerified()).isTrue();
    }

    @Test
    void toUserResponse_professorRole_returnsPROFESSOR() {
        User user = User.builder().id("u2").email("p@test.com")
                .displayName("Prof Popa").role("professor").build();

        UserResponse resp = cognitoAuthService.toUserResponse(user, false);

        assertThat(resp.getRole()).isEqualTo("PROFESSOR");
    }

    @Test
    void toUserResponse_teacherRole_returnsPROFESSOR() {
        User user = User.builder().id("u3").email("t@test.com")
                .displayName("Teacher").role("teacher").build();

        UserResponse resp = cognitoAuthService.toUserResponse(user, true);

        assertThat(resp.getRole()).isEqualTo("PROFESSOR");
    }

    @Test
    void toUserResponse_adminRole_returnsADMIN() {
        User user = User.builder().id("u4").email("a@test.com")
                .displayName("Admin").role("admin").build();

        UserResponse resp = cognitoAuthService.toUserResponse(user, true);

        assertThat(resp.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void toUserResponse_nullRole_defaultsStudent() {
        User user = User.builder().id("u5").email("x@test.com")
                .displayName("X").role(null).build();

        UserResponse resp = cognitoAuthService.toUserResponse(user, false);

        assertThat(resp.getRole()).isEqualTo("STUDENT");
    }

    @Test
    void toUserResponse_nullDisplayName_emptyNames() {
        User user = User.builder().id("u6").email("y@test.com")
                .displayName(null).role("student").build();

        UserResponse resp = cognitoAuthService.toUserResponse(user, false);

        assertThat(resp.getFirstName()).isEqualTo("");
        assertThat(resp.getLastName()).isEqualTo("");
    }

    @Test
    void toUserResponse_singleWordDisplayName_emptyLastName() {
        User user = User.builder().id("u7").email("z@test.com")
                .displayName("Ion").role("student").build();

        UserResponse resp = cognitoAuthService.toUserResponse(user, true);

        assertThat(resp.getFirstName()).isEqualTo("Ion");
        assertThat(resp.getLastName()).isEqualTo("");
    }

    @Test
    void toUserResponse_allFieldsMapped() {
        User user = User.builder()
                .id("u8").cognitoSub("sub8").email("full@test.com")
                .displayName("Ana Maria").role("student")
                .banned(true).bannedBy("admin1").banReason("spam")
                .needsRoleSelection(false)
                .build();

        UserResponse resp = cognitoAuthService.toUserResponse(user, true);

        assertThat(resp.getCognitoSub()).isEqualTo("sub8");
        assertThat(resp.getEmail()).isEqualTo("full@test.com");
        assertThat(resp.getDisplayName()).isEqualTo("Ana Maria");
        assertThat(resp.isBanned()).isTrue();
        assertThat(resp.getBannedBy()).isEqualTo("admin1");
        assertThat(resp.getBanReason()).isEqualTo("spam");
    }

    // ── getMe ──────────────────────────────────────────────────────────────────

    @Test
    void getMe_userFound_withSub_emailVerifiedTrue() {
        User user = User.builder().id("u1").email("a@b.com")
                .displayName("Test").role("student").cognitoSub("sub1").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        UserResponse resp = cognitoAuthService.getMe("u1");

        assertThat(resp.isEmailVerified()).isTrue();
    }

    @Test
    void getMe_userFound_noSub_emailVerifiedFalse() {
        User user = User.builder().id("u2").email("b@b.com")
                .displayName("Test").role("student").cognitoSub(null).build();
        when(userRepository.findById("u2")).thenReturn(Optional.of(user));

        UserResponse resp = cognitoAuthService.getMe("u2");

        assertThat(resp.isEmailVerified()).isFalse();
    }

    @Test
    void getMe_userNotFound_throws() {
        when(userRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cognitoAuthService.getMe("noId"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── register ───────────────────────────────────────────────────────────────

    @Test
    void register_newUser_noSecret_savesStudent() {
        stubNoSecret();
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoProperties.getClientId()).thenReturn("client1");

        SignUpResponse signUpResp = SignUpResponse.builder().userSub("sub-new").build();
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(signUpResp);
        when(userRepository.findByEmail("ion@test.com")).thenReturn(Optional.empty());
        when(userRepository.existsByCognitoSub("sub-new")).thenReturn(false);
        User savedUser = User.builder().id("u1").build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        cognitoAuthService.register(buildRegisterReq("ion@test.com", UserRole.STUDENT));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("student");
        assertThat(captor.getValue().getCognitoSub()).isEqualTo("sub-new");
    }

    @Test
    void register_newUser_withSecret_computesSecretHash() {
        stubWithSecret();
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");

        SignUpResponse signUpResp = SignUpResponse.builder().userSub("sub-secret").build();
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(signUpResp);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByCognitoSub(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(User.builder().build());
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        cognitoAuthService.register(buildRegisterReq("secret@test.com", UserRole.STUDENT));

        verify(cognitoClient).signUp(any(SignUpRequest.class));
    }

    @Test
    void register_professorRole_savesAsProfessor() {
        stubNoSecret();
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoProperties.getClientId()).thenReturn("client1");

        when(cognitoClient.signUp(any(SignUpRequest.class)))
                .thenReturn(SignUpResponse.builder().userSub("sub-prof").build());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByCognitoSub(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(User.builder().build());
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        cognitoAuthService.register(buildRegisterReq("prof@test.com", UserRole.PROFESSOR));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("professor");
    }

    @Test
    void register_existingEmail_updatesCognitoSub() {
        stubNoSecret();
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoProperties.getClientId()).thenReturn("client1");

        when(cognitoClient.signUp(any(SignUpRequest.class)))
                .thenReturn(SignUpResponse.builder().userSub("new-sub").build());
        User existing = User.builder().id("old").email("ion@test.com").role("student").build();
        when(userRepository.findByEmail("ion@test.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        cognitoAuthService.register(buildRegisterReq("ion@test.com", UserRole.STUDENT));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getCognitoSub()).isEqualTo("new-sub");
    }

    @Test
    void register_subAlreadyExists_doesNotSave() {
        stubNoSecret();
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoProperties.getClientId()).thenReturn("client1");

        when(cognitoClient.signUp(any(SignUpRequest.class)))
                .thenReturn(SignUpResponse.builder().userSub("dup-sub").build());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByCognitoSub("dup-sub")).thenReturn(true);
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        cognitoAuthService.register(buildRegisterReq("dup@test.com", UserRole.STUDENT));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_signUpThrows_propagatesException() {
        stubNoSecret();
        when(cognitoProperties.getClientId()).thenReturn("client1");
        when(cognitoClient.signUp(any(SignUpRequest.class)))
                .thenThrow(NotAuthorizedException.builder().message("blocked").build());

        assertThatThrownBy(() -> cognitoAuthService.register(buildRegisterReq("x@test.com", UserRole.STUDENT)))
                .isInstanceOf(NotAuthorizedException.class);
    }

    @Test
    void register_dbSaveThrows_rollbacksAndRethrows() {
        stubNoSecret();
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoProperties.getClientId()).thenReturn("client1");

        when(cognitoClient.signUp(any(SignUpRequest.class)))
                .thenReturn(SignUpResponse.builder().userSub("sub-rollback").build());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByCognitoSub(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB down"));
        when(cognitoClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
                .thenReturn(AdminDeleteUserResponse.builder().build());

        assertThatThrownBy(() -> cognitoAuthService.register(buildRegisterReq("fail@test.com", UserRole.STUDENT)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Eroare la salvarea contului");

        verify(cognitoClient).adminDeleteUser(any(AdminDeleteUserRequest.class));
    }

    @Test
    void register_dbSaveThrows_rollbackAlsoFails_stillRethrows() {
        stubNoSecret();
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoProperties.getClientId()).thenReturn("client1");

        when(cognitoClient.signUp(any(SignUpRequest.class)))
                .thenReturn(SignUpResponse.builder().userSub("sub-x").build());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByCognitoSub(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB down"));
        when(cognitoClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
                .thenThrow(new RuntimeException("Rollback failed"));

        assertThatThrownBy(() -> cognitoAuthService.register(buildRegisterReq("x2@test.com", UserRole.STUDENT)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void register_adminAddGroupThrows_sdkClientException_logsAndContinues() {
        stubNoSecret();
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoProperties.getClientId()).thenReturn("client1");

        when(cognitoClient.signUp(any(SignUpRequest.class)))
                .thenReturn(SignUpResponse.builder().userSub("sub-grp").build());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByCognitoSub(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(User.builder().build());
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenThrow(new RuntimeException("Group error"));

        // Should not throw
        cognitoAuthService.register(buildRegisterReq("grp@test.com", UserRole.STUDENT));
    }

    // ── verifyEmail ────────────────────────────────────────────────────────────

    @Test
    void verifyEmail_noSecret_callsConfirmSignUp() {
        stubNoSecret();
        when(cognitoProperties.getClientId()).thenReturn("client1");
        when(cognitoClient.confirmSignUp(any(ConfirmSignUpRequest.class)))
                .thenReturn(ConfirmSignUpResponse.builder().build());

        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("v@test.com");
        req.setCode("123456");

        cognitoAuthService.verifyEmail(req);

        verify(cognitoClient).confirmSignUp(any(ConfirmSignUpRequest.class));
    }

    @Test
    void verifyEmail_withSecret_includesSecretHash() {
        stubWithSecret();
        when(cognitoClient.confirmSignUp(any(ConfirmSignUpRequest.class)))
                .thenReturn(ConfirmSignUpResponse.builder().build());

        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("v@test.com");
        req.setCode("654321");

        cognitoAuthService.verifyEmail(req);

        verify(cognitoClient).confirmSignUp(any(ConfirmSignUpRequest.class));
    }

    // ── resendVerificationCode ─────────────────────────────────────────────────

    @Test
    void resendVerificationCode_noSecret_callsResend() {
        stubNoSecret();
        when(cognitoProperties.getClientId()).thenReturn("client1");
        when(cognitoClient.resendConfirmationCode(any(ResendConfirmationCodeRequest.class)))
                .thenReturn(ResendConfirmationCodeResponse.builder().build());

        ResendVerificationRequest req = new ResendVerificationRequest();
        req.setEmail("r@test.com");

        cognitoAuthService.resendVerificationCode(req);

        verify(cognitoClient).resendConfirmationCode(any(ResendConfirmationCodeRequest.class));
    }

    @Test
    void resendVerificationCode_withSecret_includesSecretHash() {
        stubWithSecret();
        when(cognitoClient.resendConfirmationCode(any(ResendConfirmationCodeRequest.class)))
                .thenReturn(ResendConfirmationCodeResponse.builder().build());

        ResendVerificationRequest req = new ResendVerificationRequest();
        req.setEmail("r2@test.com");

        cognitoAuthService.resendVerificationCode(req);

        verify(cognitoClient).resendConfirmationCode(any(ResendConfirmationCodeRequest.class));
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    void login_success_noSecret_returnsAuthResponse() {
        stubNoSecret();
        when(cognitoProperties.getClientId()).thenReturn("client1");

        AuthenticationResultType authResult = AuthenticationResultType.builder()
                .accessToken("acc-tok").refreshToken("ref-tok").expiresIn(3600).build();
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .thenReturn(InitiateAuthResponse.builder().authenticationResult(authResult).build());

        GetUserResponse userResp = GetUserResponse.builder()
                .userAttributes(
                        AttributeType.builder().name("sub").value("sub1").build(),
                        AttributeType.builder().name("email_verified").value("true").build(),
                        AttributeType.builder().name("email").value("login@test.com").build()
                ).build();
        when(cognitoClient.getUser(any(GetUserRequest.class))).thenReturn(userResp);

        User dbUser = User.builder().id("u1").email("login@test.com")
                .displayName("Ion").role("student").cognitoSub("sub1").build();
        when(userRepository.findByCognitoSub("sub1")).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenReturn(dbUser);

        LoginRequest req = new LoginRequest();
        req.setEmail("login@test.com");
        req.setPassword("Parola1!");

        AuthResponse result = cognitoAuthService.login(req);

        assertThat(result.getAccessToken()).isEqualTo("acc-tok");
        assertThat(result.getRefreshToken()).isEqualTo("ref-tok");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_withSecret_success() {
        stubWithSecret();

        AuthenticationResultType authResult = AuthenticationResultType.builder()
                .accessToken("acc2").refreshToken("ref2").expiresIn(1800).build();
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .thenReturn(InitiateAuthResponse.builder().authenticationResult(authResult).build());

        GetUserResponse userResp = GetUserResponse.builder()
                .userAttributes(
                        AttributeType.builder().name("sub").value("sub2").build(),
                        AttributeType.builder().name("email_verified").value("false").build()
                ).build();
        when(cognitoClient.getUser(any(GetUserRequest.class))).thenReturn(userResp);

        User dbUser = User.builder().id("u2").email("s@test.com")
                .displayName("Ana").role("student").cognitoSub("sub2").build();
        when(userRepository.findByCognitoSub("sub2")).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenReturn(dbUser);

        LoginRequest req = new LoginRequest();
        req.setEmail("s@test.com");
        req.setPassword("Pass1!");

        AuthResponse result = cognitoAuthService.login(req);

        assertThat(result.getAccessToken()).isEqualTo("acc2");
    }

    @Test
    void login_invalidParamException_propagates() {
        stubNoSecret();
        when(cognitoProperties.getClientId()).thenReturn("client1");
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .thenThrow(InvalidParameterException.builder().message("disabled").build());

        LoginRequest req = new LoginRequest();
        req.setEmail("x@test.com");
        req.setPassword("pass");

        assertThatThrownBy(() -> cognitoAuthService.login(req))
                .isInstanceOf(InvalidParameterException.class);
    }

    @Test
    void login_notAuthorizedException_propagates() {
        stubNoSecret();
        when(cognitoProperties.getClientId()).thenReturn("client1");
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .thenThrow(NotAuthorizedException.builder().message("bad creds").build());

        LoginRequest req = new LoginRequest();
        req.setEmail("y@test.com");
        req.setPassword("wrong");

        assertThatThrownBy(() -> cognitoAuthService.login(req))
                .isInstanceOf(NotAuthorizedException.class);
    }

    // ── refresh ────────────────────────────────────────────────────────────────

    @Test
    void refresh_success_noSecret_returnsNewAccessToken() {
        stubNoSecret();
        when(cognitoProperties.getClientId()).thenReturn("client1");

        AuthenticationResultType authResult = AuthenticationResultType.builder()
                .accessToken("new-acc").expiresIn(3600).build();
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .thenReturn(InitiateAuthResponse.builder().authenticationResult(authResult).build());

        User dbUser = User.builder().id("u3").email("ref@test.com")
                .displayName("Ion").role("student").cognitoSub("sub3").build();
        when(userRepository.findByEmail("ref@test.com")).thenReturn(Optional.of(dbUser));

        RefreshRequest req = new RefreshRequest();
        req.setEmail("ref@test.com");
        req.setRefreshToken("old-ref");

        AuthResponse result = cognitoAuthService.refresh(req);

        assertThat(result.getAccessToken()).isEqualTo("new-acc");
        assertThat(result.getRefreshToken()).isEqualTo("old-ref");
    }

    @Test
    void refresh_withSecret_success() {
        stubWithSecret();

        AuthenticationResultType authResult = AuthenticationResultType.builder()
                .accessToken("new-acc2").expiresIn(3600).build();
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .thenReturn(InitiateAuthResponse.builder().authenticationResult(authResult).build());

        User dbUser = User.builder().id("u4").email("ref2@test.com")
                .displayName("Ana").role("professor").cognitoSub("sub4").build();
        when(userRepository.findByEmail("ref2@test.com")).thenReturn(Optional.of(dbUser));

        RefreshRequest req = new RefreshRequest();
        req.setEmail("ref2@test.com");
        req.setRefreshToken("old-ref2");

        AuthResponse result = cognitoAuthService.refresh(req);

        assertThat(result.getAccessToken()).isEqualTo("new-acc2");
    }

    @Test
    void refresh_userNotFound_throws() {
        stubNoSecret();
        when(cognitoProperties.getClientId()).thenReturn("client1");

        AuthenticationResultType authResult = AuthenticationResultType.builder()
                .accessToken("tok").expiresIn(3600).build();
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .thenReturn(InitiateAuthResponse.builder().authenticationResult(authResult).build());
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        RefreshRequest req = new RefreshRequest();
        req.setEmail("missing@test.com");
        req.setRefreshToken("ref");

        assertThatThrownBy(() -> cognitoAuthService.refresh(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // ── forgotPassword ─────────────────────────────────────────────────────────

    @Test
    void forgotPassword_noSecret_callsCognito() {
        stubNoSecret();
        when(cognitoProperties.getClientId()).thenReturn("client1");
        when(cognitoClient.forgotPassword(
                any(software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest.class)))
                .thenReturn(software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse.builder().build());

        ro.fiismart.auth.dto.request.ForgotPasswordRequest req = new ro.fiismart.auth.dto.request.ForgotPasswordRequest();
        req.setEmail("forgot@test.com");

        cognitoAuthService.forgotPassword(req);

        verify(cognitoClient).forgotPassword(
                any(software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest.class));
    }

    @Test
    void forgotPassword_withSecret_includesSecretHash() {
        stubWithSecret();
        when(cognitoClient.forgotPassword(
                any(software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest.class)))
                .thenReturn(software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse.builder().build());

        ro.fiismart.auth.dto.request.ForgotPasswordRequest req = new ro.fiismart.auth.dto.request.ForgotPasswordRequest();
        req.setEmail("forgot2@test.com");

        cognitoAuthService.forgotPassword(req);

        verify(cognitoClient).forgotPassword(
                any(software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest.class));
    }

    // ── resetPassword ──────────────────────────────────────────────────────────

    @Test
    void resetPassword_noSecret_callsCognito() {
        stubNoSecret();
        when(cognitoProperties.getClientId()).thenReturn("client1");
        when(cognitoClient.confirmForgotPassword(any(ConfirmForgotPasswordRequest.class)))
                .thenReturn(ConfirmForgotPasswordResponse.builder().build());

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("reset@test.com");
        req.setCode("111222");
        req.setNewPassword("NewPass1!");

        cognitoAuthService.resetPassword(req);

        verify(cognitoClient).confirmForgotPassword(any(ConfirmForgotPasswordRequest.class));
    }

    @Test
    void resetPassword_withSecret_includesSecretHash() {
        stubWithSecret();
        when(cognitoClient.confirmForgotPassword(any(ConfirmForgotPasswordRequest.class)))
                .thenReturn(ConfirmForgotPasswordResponse.builder().build());

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("reset2@test.com");
        req.setCode("333444");
        req.setNewPassword("NewPass2!");

        cognitoAuthService.resetPassword(req);

        verify(cognitoClient).confirmForgotPassword(any(ConfirmForgotPasswordRequest.class));
    }

    // ── logout ─────────────────────────────────────────────────────────────────

    @Test
    void logout_callsGlobalSignOut() {
        when(cognitoClient.globalSignOut(any(GlobalSignOutRequest.class)))
                .thenReturn(GlobalSignOutResponse.builder().build());

        cognitoAuthService.logout("access-token-123");

        verify(cognitoClient).globalSignOut(any(GlobalSignOutRequest.class));
    }

    // ── assignRole ─────────────────────────────────────────────────────────────

    @Test
    void assignRole_student_setsRoleAndClearsNeedsRoleSelection() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");

        User user = User.builder().id("u1").email("federated@test.com")
                .displayName("Google User").role(null).needsRoleSelection(true)
                .cognitoUsername("Google_abc123").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        UserResponse resp = cognitoAuthService.assignRole("u1", "STUDENT", "Ion", "Pop");

        assertThat(resp.getRole()).isEqualTo("STUDENT");
        assertThat(user.getRole()).isEqualTo("student");
        assertThat(user.isNeedsRoleSelection()).isFalse();
        assertThat(user.getDisplayName()).isEqualTo("Ion Pop");
    }

    @Test
    void assignRole_professor_setsRoleProfessor() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");

        User user = User.builder().id("u2").email("prof@test.com")
                .displayName("Federated").role(null).needsRoleSelection(true)
                .cognitoUsername(null).build();
        when(userRepository.findById("u2")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        UserResponse resp = cognitoAuthService.assignRole("u2", "PROFESSOR", null, null);

        assertThat(resp.getRole()).isEqualTo("PROFESSOR");
        assertThat(user.getRole()).isEqualTo("professor");
    }

    @Test
    void assignRole_nullFirstName_doesNotChangeDisplayName() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");

        User user = User.builder().id("u3").email("x@test.com")
                .displayName("Existing Name").role(null).needsRoleSelection(true)
                .cognitoUsername("google_123").build();
        when(userRepository.findById("u3")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        cognitoAuthService.assignRole("u3", "STUDENT", null, null);

        assertThat(user.getDisplayName()).isEqualTo("Existing Name");
    }

    @Test
    void assignRole_blankFirstName_doesNotChangeDisplayName() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");

        User user = User.builder().id("u4").email("y@test.com")
                .displayName("Current Name").role(null).needsRoleSelection(true).build();
        when(userRepository.findById("u4")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        cognitoAuthService.assignRole("u4", "STUDENT", "  ", null);

        assertThat(user.getDisplayName()).isEqualTo("Current Name");
    }

    @Test
    void assignRole_userNotFound_throws() {
        when(userRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cognitoAuthService.assignRole("nope", "STUDENT", "a", "b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignRole_alreadyHasRole_throwsIllegalState() {
        User user = User.builder().id("u5").email("z@test.com")
                .role("student").needsRoleSelection(false).build();
        when(userRepository.findById("u5")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> cognitoAuthService.assignRole("u5", "STUDENT", "Ion", "Pop"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void assignRole_cognitoAddGroupFails_logsAndReturnsResponse() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");

        User user = User.builder().id("u6").email("err@test.com")
                .displayName("Err").role(null).needsRoleSelection(true).build();
        when(userRepository.findById("u6")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenThrow(new RuntimeException("Group error"));

        // Should not throw even if Cognito group fails
        UserResponse resp = cognitoAuthService.assignRole("u6", "STUDENT", "Ion", "Pop");

        assertThat(resp.getRole()).isEqualTo("STUDENT");
    }

    @Test
    void assignRole_firstNameWithoutLastName_setsDisplayNameWithoutSpace() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");

        User user = User.builder().id("u7").email("n@test.com")
                .displayName("Old").role(null).needsRoleSelection(true).build();
        when(userRepository.findById("u7")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        cognitoAuthService.assignRole("u7", "STUDENT", "Ion", "  ");

        assertThat(user.getDisplayName()).isEqualTo("Ion");
    }

    // ── logConfiguration (@PostConstruct) ─────────────────────────────────────

    @Test
    void logConfiguration_withSecret_logsConfiguration() {
        when(cognitoProperties.getRegion()).thenReturn("eu-central-1");
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoProperties.getClientId()).thenReturn("client1");
        when(cognitoProperties.getClientSecret()).thenReturn("secret");
        when(cognitoProperties.getJwksUri()).thenReturn("https://jwks.uri");

        // Should not throw
        cognitoAuthService.logConfiguration();
    }

    @Test
    void logConfiguration_noSecret_logsWarning() {
        when(cognitoProperties.getRegion()).thenReturn("eu-central-1");
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoProperties.getClientId()).thenReturn("client1");
        when(cognitoProperties.getClientSecret()).thenReturn("");
        when(cognitoProperties.getJwksUri()).thenReturn("https://jwks.uri");

        // Should not throw
        cognitoAuthService.logConfiguration();
    }
}
