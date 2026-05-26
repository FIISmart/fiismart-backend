package ro.fiismart.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.fiismart.common.config.CognitoProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoAdminService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final CognitoProperties cognitoProperties;

    public void createUser(String email, String temporaryPassword, String role) {
        try {
            cognitoClient.adminCreateUser(AdminCreateUserRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(email)
                    .temporaryPassword(temporaryPassword)
                    .messageAction(MessageActionType.SUPPRESS)
                    .userAttributes(
                            AttributeType.builder().name("email").value(email).build(),
                            AttributeType.builder().name("email_verified").value("true").build()
                    )
                    .build());

            String groupName = role.toUpperCase();
            cognitoClient.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(email)
                    .groupName(groupName)
                    .build());

            log.info("Utilizator Cognito creat: {} cu rolul {}", email, groupName);
        } catch (UsernameExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Un utilizator cu email-ul '" + email + "' există deja în Cognito.");
        } catch (InvalidPasswordException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Parola temporară nu respectă politica Cognito: " + e.getMessage());
        } catch (Exception e) {
            log.error("Eroare la crearea utilizatorului Cognito: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Eroare la crearea utilizatorului în Cognito.");
        }
    }

    public void deleteUser(String email) {
        try {
            cognitoClient.adminDeleteUser(AdminDeleteUserRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(email)
                    .build());
            log.info("Utilizator Cognito șters: {}", email);
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Utilizatorul '" + email + "' nu există în Cognito.");
        }
    }

    public void forcePasswordReset(String email) {
        try {
            cognitoClient.adminResetUserPassword(AdminResetUserPasswordRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(email)
                    .build());
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Utilizatorul '" + email + "' nu există în Cognito.");
        }
    }

    public void addUserToGroup(String username, String groupName) {
        try {
            cognitoClient.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(username)
                    .groupName(groupName)
                    .build());
            log.info("Utilizator {} adăugat în grupul Cognito: {}", username, groupName);
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Utilizatorul '" + username + "' nu există în Cognito.");
        } catch (Exception e) {
            log.error("Eroare la adăugarea în grupul Cognito {}: {}", groupName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Eroare la modificarea grupului în Cognito.");
        }
    }

    public void removeUserFromGroup(String username, String groupName) {
        try {
            cognitoClient.adminRemoveUserFromGroup(AdminRemoveUserFromGroupRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(username)
                    .groupName(groupName)
                    .build());
            log.info("Utilizator {} eliminat din grupul Cognito: {}", username, groupName);
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Utilizatorul '" + username + "' nu există în Cognito.");
        } catch (Exception e) {
            log.error("Eroare la eliminarea din grupul Cognito {}: {}", groupName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Eroare la modificarea grupului în Cognito.");
        }
    }

    public List<UserType> listUsersInGroup(String groupName) {
        return cognitoClient.listUsersInGroup(ListUsersInGroupRequest.builder()
                .userPoolId(cognitoProperties.getUserPoolId())
                .groupName(groupName)
                .build())
                .users();
    }
}
