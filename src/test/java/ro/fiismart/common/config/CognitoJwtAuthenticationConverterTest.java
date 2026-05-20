package ro.fiismart.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import ro.fiismart.auth.service.CognitoUserSyncService;
import ro.fiismart.common.model.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitoJwtAuthenticationConverterTest {

    @Mock
    private CognitoUserSyncService cognitoUserSyncService;

    @InjectMocks
    private CognitoJwtAuthenticationConverter converter;

    private Jwt buildJwt(String sub, String cognitoUsername, String email, String name, List<String> groups) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(sub);
        when(jwt.getClaimAsString("cognito:username")).thenReturn(cognitoUsername);
        when(jwt.getClaimAsString("email")).thenReturn(email);
        when(jwt.getClaimAsString("name")).thenReturn(name);
        when(jwt.getClaimAsStringList("cognito:groups")).thenReturn(groups);
        return jwt;
    }

    // ── Roluri ─────────────────────────────────────────────────────────────────

    @Test
    void convert_studentRole_grantsROLE_STUDENT() {
        Jwt jwt = buildJwt("sub1", "user@test.com", "user@test.com", "Ion", List.of("STUDENT"));
        User user = User.builder().id("u1").role("student").build();
        when(cognitoUserSyncService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(user);

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getPrincipal()).isEqualTo("u1");
        assertThat(token.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void convert_professorRole_grantsROLE_PROFESSOR() {
        Jwt jwt = buildJwt("sub2", "prof@test.com", "prof@test.com", "Prof", List.of("PROFESSOR"));
        User user = User.builder().id("u2").role("professor").build();
        when(cognitoUserSyncService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(user);

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_PROFESSOR");
    }

    @Test
    void convert_teacherRole_grantsROLE_PROFESSOR() {
        Jwt jwt = buildJwt("sub3", "teacher@test.com", "teacher@test.com", "Teacher", List.of());
        User user = User.builder().id("u3").role("teacher").build();
        when(cognitoUserSyncService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(user);

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_PROFESSOR");
    }

    @Test
    void convert_adminRole_grantsROLE_ADMIN() {
        Jwt jwt = buildJwt("sub4", "admin@test.com", "admin@test.com", "Admin", List.of("ADMIN"));
        User user = User.builder().id("u4").role("admin").build();
        when(cognitoUserSyncService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(user);

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void convert_nullRole_defaultsROLE_STUDENT() {
        Jwt jwt = buildJwt("sub5", "no-role@test.com", "no-role@test.com", "X", null);
        User user = User.builder().id("u5").role(null).build();
        when(cognitoUserSyncService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(user);

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_STUDENT");
    }

    // ── Câmpuri lipsă ──────────────────────────────────────────────────────────

    @Test
    void convert_noCognitoUsername_fallsBackToUsernameField() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("sub6");
        when(jwt.getClaimAsString("cognito:username")).thenReturn(null);
        when(jwt.getClaimAsString("username")).thenReturn("fallback@test.com");
        when(jwt.getClaimAsString("email")).thenReturn("fallback@test.com");
        when(jwt.getClaimAsString("name")).thenReturn("Fallback");
        when(jwt.getClaimAsStringList("cognito:groups")).thenReturn(List.of());

        User user = User.builder().id("u6").role("student").build();
        when(cognitoUserSyncService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(user);

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
    }

    @Test
    void convert_blankCognitoUsername_fallsBackToUsernameField() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("sub7");
        when(jwt.getClaimAsString("cognito:username")).thenReturn("   ");
        when(jwt.getClaimAsString("username")).thenReturn("blank@test.com");
        when(jwt.getClaimAsString("email")).thenReturn("blank@test.com");
        when(jwt.getClaimAsString("name")).thenReturn("Blank");
        when(jwt.getClaimAsStringList("cognito:groups")).thenReturn(List.of());

        User user = User.builder().id("u7").role("student").build();
        when(cognitoUserSyncService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(user);

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
    }

    @Test
    void convert_noEmailInJwt_emailDerivedFromCognitoUsername() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("sub8");
        when(jwt.getClaimAsString("cognito:username")).thenReturn("derived@test.com");
        when(jwt.getClaimAsString("email")).thenReturn(null);
        when(jwt.getClaimAsString("name")).thenReturn("Derived");
        when(jwt.getClaimAsStringList("cognito:groups")).thenReturn(List.of());

        User user = User.builder().id("u8").role("student").build();
        when(cognitoUserSyncService.syncUser(
                eq("derived@test.com"), eq("sub8"), eq("derived@test.com"),
                eq("Derived"), eq(List.of()), eq(false))
        ).thenReturn(user);

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
    }

    @Test
    void convert_federatedUser_noAtSign_isFederatedTrue() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("sub9");
        when(jwt.getClaimAsString("cognito:username")).thenReturn("Google_abc123");
        when(jwt.getClaimAsString("email")).thenReturn("google@gmail.com");
        when(jwt.getClaimAsString("name")).thenReturn("Google User");
        when(jwt.getClaimAsStringList("cognito:groups")).thenReturn(List.of());

        User user = User.builder().id("u9").role("student").build();
        when(cognitoUserSyncService.syncUser(
                eq("google@gmail.com"), eq("sub9"), eq("Google_abc123"),
                eq("Google User"), eq(List.of()), eq(true))
        ).thenReturn(user);

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
    }

    @Test
    void convert_nullGroups_usesEmptyList() {
        Jwt jwt = buildJwt("sub10", "ng@test.com", "ng@test.com", "NoGroups", null);
        User user = User.builder().id("u10").role("student").build();
        when(cognitoUserSyncService.syncUser(
                any(), any(), any(), any(), eq(List.of()), anyBoolean())
        ).thenReturn(user);

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
    }

    @Test
    void convert_syncThrowsException_returnsNull() {
        Jwt jwt = buildJwt("sub11", "err@test.com", "err@test.com", "Err", List.of());
        when(cognitoUserSyncService.syncUser(any(), any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new RuntimeException("sync failed"));

        UsernamePasswordAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNull();
    }
}
