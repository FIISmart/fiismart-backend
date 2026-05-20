package ro.fiismart.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ro.fiismart.common.config.CognitoProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitoAdminServiceTest {

    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    @Mock
    private CognitoProperties cognitoProperties;

    @InjectMocks
    private CognitoAdminService service;

    // ── createUser ─────────────────────────────────────────────────────────────

    @Test
    void createUser_success() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenReturn(AdminCreateUserResponse.builder().build());
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        service.createUser("test@test.com", "TempPass1!", "STUDENT");

        verify(cognitoClient).adminCreateUser(any(AdminCreateUserRequest.class));
        verify(cognitoClient).adminAddUserToGroup(any(AdminAddUserToGroupRequest.class));
    }

    @Test
    void createUser_usernameExists_throws409() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenThrow(UsernameExistsException.builder().message("exists").build());

        assertThatThrownBy(() -> service.createUser("dup@test.com", "Pass1!", "STUDENT"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createUser_invalidPassword_throws400() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenThrow(InvalidPasswordException.builder().message("weak").build());

        assertThatThrownBy(() -> service.createUser("test@test.com", "weak", "STUDENT"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createUser_genericException_throws500() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenThrow(new RuntimeException("unexpected"));

        assertThatThrownBy(() -> service.createUser("test@test.com", "Pass1!", "STUDENT"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    // ── deleteUser ─────────────────────────────────────────────────────────────

    @Test
    void deleteUser_success() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
                .thenReturn(AdminDeleteUserResponse.builder().build());

        service.deleteUser("test@test.com");

        verify(cognitoClient).adminDeleteUser(any(AdminDeleteUserRequest.class));
    }

    @Test
    void deleteUser_userNotFound_logsAndContinues() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
                .thenThrow(UserNotFoundException.builder().message("not found").build());

        // Should not throw
        service.deleteUser("ghost@test.com");
    }

    @Test
    void deleteUser_genericException_logsAndContinues() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
                .thenThrow(new RuntimeException("error"));

        // Should not throw
        service.deleteUser("test@test.com");
    }

    // ── forcePasswordReset ─────────────────────────────────────────────────────

    @Test
    void forcePasswordReset_success() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminResetUserPassword(any(AdminResetUserPasswordRequest.class)))
                .thenReturn(AdminResetUserPasswordResponse.builder().build());

        service.forcePasswordReset("test@test.com");

        verify(cognitoClient).adminResetUserPassword(any(AdminResetUserPasswordRequest.class));
    }

    @Test
    void forcePasswordReset_userNotFound_throws404() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminResetUserPassword(any(AdminResetUserPasswordRequest.class)))
                .thenThrow(UserNotFoundException.builder().message("not found").build());

        assertThatThrownBy(() -> service.forcePasswordReset("ghost@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── listUsersInGroup ───────────────────────────────────────────────────────

    @Test
    void listUsersInGroup_returnsUsers() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        UserType user = UserType.builder().username("test@test.com").build();
        when(cognitoClient.listUsersInGroup(any(ListUsersInGroupRequest.class)))
                .thenReturn(ListUsersInGroupResponse.builder().users(List.of(user)).build());

        List<UserType> result = service.listUsersInGroup("STUDENT");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("test@test.com");
    }

    // ── addUserToGroup ─────────────────────────────────────────────────────────

    @Test
    void addUserToGroup_success() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());

        service.addUserToGroup("test@test.com", "PROFESSOR");

        verify(cognitoClient).adminAddUserToGroup(any(AdminAddUserToGroupRequest.class));
    }

    @Test
    void addUserToGroup_exception_logsAndContinues() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                .thenThrow(new RuntimeException("error"));

        // Should not throw
        service.addUserToGroup("test@test.com", "PROFESSOR");
    }

    // ── removeUserFromGroup ────────────────────────────────────────────────────

    @Test
    void removeUserFromGroup_success() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminRemoveUserFromGroup(any(AdminRemoveUserFromGroupRequest.class)))
                .thenReturn(AdminRemoveUserFromGroupResponse.builder().build());

        service.removeUserFromGroup("test@test.com", "STUDENT");

        verify(cognitoClient).adminRemoveUserFromGroup(any(AdminRemoveUserFromGroupRequest.class));
    }

    @Test
    void removeUserFromGroup_exception_logsAndContinues() {
        when(cognitoProperties.getUserPoolId()).thenReturn("pool1");
        when(cognitoClient.adminRemoveUserFromGroup(any(AdminRemoveUserFromGroupRequest.class)))
                .thenThrow(new RuntimeException("error"));

        // Should not throw
        service.removeUserFromGroup("test@test.com", "STUDENT");
    }
}
