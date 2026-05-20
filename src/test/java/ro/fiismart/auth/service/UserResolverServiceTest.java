package ro.fiismart.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserResolverServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private Jwt jwt;

    @InjectMocks
    private UserResolverService userResolverService;

    @Test
    void resolveUser_validEmail_returnsUser() {
        User user = User.builder().id("u1").email("test@example.com").build();
        when(jwt.getClaimAsString("email")).thenReturn("Test@Example.COM");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        User result = userResolverService.resolveUser(jwt);

        assertThat(result.getId()).isEqualTo("u1");
    }

    @Test
    void resolveUser_noEmailClaim_throws401() {
        when(jwt.getClaimAsString("email")).thenReturn(null);

        assertThatThrownBy(() -> userResolverService.resolveUser(jwt))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("email");
    }

    @Test
    void resolveUser_userNotFound_throws404() {
        when(jwt.getClaimAsString("email")).thenReturn("notfound@example.com");
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userResolverService.resolveUser(jwt))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void resolveMongoId_returnsId() {
        User user = User.builder().id("mongo123").email("a@b.com").build();
        when(jwt.getClaimAsString("email")).thenReturn("a@b.com");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        String id = userResolverService.resolveMongoId(jwt);

        assertThat(id).isEqualTo("mongo123");
    }

    @Test
    void validateOwnership_sameUser_noException() {
        User user = User.builder().id("u1").email("owner@x.com").build();
        when(jwt.getClaimAsString("email")).thenReturn("owner@x.com");
        when(userRepository.findByEmail("owner@x.com")).thenReturn(Optional.of(user));

        assertThatCode(() -> userResolverService.validateOwnership("u1", jwt))
                .doesNotThrowAnyException();
    }

    @Test
    void validateOwnership_differentUser_throws403() {
        User user = User.builder().id("u1").email("other@x.com").build();
        when(jwt.getClaimAsString("email")).thenReturn("other@x.com");
        when(userRepository.findByEmail("other@x.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userResolverService.validateOwnership("u2", jwt))
                .isInstanceOf(ResponseStatusException.class);
    }
}
