package ro.fiismart.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitoUserSyncServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private CognitoUserSyncService cognitoUserSyncService;

    @Test
    void syncUser_foundBySub_returnsDirect() {
        User existing = User.builder().id("u1").cognitoSub("sub1").cognitoUsername("user1").build();
        when(userRepository.findByCognitoSub("sub1")).thenReturn(Optional.of(existing));

        User result = cognitoUserSyncService.syncUser("a@b.com", "sub1", "user1", "Name", List.of(), false);

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void syncUser_foundBySub_missingUsername_savesUpdate() {
        User existing = User.builder().id("u1").cognitoSub("sub1").cognitoUsername(null).build();
        when(userRepository.findByCognitoSub("sub1")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = cognitoUserSyncService.syncUser("a@b.com", "sub1", "newUser", "Name", List.of(), false);

        assertThat(result).isSameAs(existing);
        verify(userRepository).save(existing);
    }

    @Test
    void syncUser_notBySub_foundByEmail_noSub_backfillsSub() {
        User existing = User.builder().id("u1").email("a@b.com").cognitoSub(null).build();
        when(userRepository.findByCognitoSub("sub2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = cognitoUserSyncService.syncUser("A@B.COM", "sub2", "usr", "Name", List.of(), false);

        assertThat(result.getCognitoSub()).isEqualTo("sub2");
        verify(userRepository).save(existing);
    }

    @Test
    void syncUser_federatedAccountLinking_updatesSub() {
        User existing = User.builder().id("u1").email("a@b.com").cognitoSub("oldSub").build();
        when(userRepository.findByCognitoSub("newSub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = cognitoUserSyncService.syncUser("a@b.com", "newSub", "usr", "Name", List.of(), true);

        assertThat(result.getCognitoSub()).isEqualTo("newSub");
    }

    @Test
    void syncUser_emailExists_differentSub_notFederated_returnsExisting() {
        User existing = User.builder().id("u1").email("a@b.com").cognitoSub("otherSub").build();
        when(userRepository.findByCognitoSub("sub3")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));

        User result = cognitoUserSyncService.syncUser("a@b.com", "sub3", "usr", "Name", List.of(), false);

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void syncUser_newNativeUser_studentRole() {
        when(userRepository.findByCognitoSub("sub4")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@b.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        cognitoUserSyncService.syncUser("new@b.com", "sub4", "usr", "Name",
                List.of("STUDENT"), false);

        assertThat(captor.getValue().getRole()).isEqualTo("student");
        assertThat(captor.getValue().isNeedsRoleSelection()).isFalse();
    }

    @Test
    void syncUser_newNativeUser_adminRole() {
        when(userRepository.findByCognitoSub("sub5")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@b.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        cognitoUserSyncService.syncUser("admin@b.com", "sub5", "usr", "Admin",
                List.of("ADMIN"), false);

        assertThat(captor.getValue().getRole()).isEqualTo("admin");
    }

    @Test
    void syncUser_newNativeUser_professorRole() {
        when(userRepository.findByCognitoSub("sub6")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("prof@b.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        cognitoUserSyncService.syncUser("prof@b.com", "sub6", "usr", "Prof",
                List.of("TEACHER"), false);

        assertThat(captor.getValue().getRole()).isEqualTo("professor");
    }

    @Test
    void syncUser_federatedNewUser_needsRoleSelection() {
        when(userRepository.findByCognitoSub("sub7")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("google@b.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        cognitoUserSyncService.syncUser("google@b.com", "sub7", "usr", "Google User",
                List.of(), true);

        assertThat(captor.getValue().isNeedsRoleSelection()).isTrue();
        assertThat(captor.getValue().getRole()).isNull();
    }

    @Test
    void syncUser_nullEmail_usesSubPlaceholder() {
        when(userRepository.findByCognitoSub("sub8")).thenReturn(Optional.empty());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        cognitoUserSyncService.syncUser(null, "sub8", "usr", "Name", List.of(), false);

        assertThat(captor.getValue().getEmail()).isEqualTo("_pending_sub8");
    }

    @Test
    void syncUser_duplicateKey_recoversExisting() {
        User recovered = User.builder().id("u9").cognitoSub("sub9").build();
        when(userRepository.findByCognitoSub("sub9")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("dup@b.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenThrow(new DuplicateKeyException("dup"));
        // Second call to findByCognitoSub during recovery
        when(userRepository.findByCognitoSub("sub9"))
                .thenReturn(Optional.empty())  // first call
                .thenReturn(Optional.of(recovered));  // recovery call

        User result = cognitoUserSyncService.syncUser("dup@b.com", "sub9", "usr", "Name", List.of(), false);

        assertThat(result).isSameAs(recovered);
    }

    @Test
    void syncUser_noGroups_defaultStudentRole() {
        when(userRepository.findByCognitoSub("sub10")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("x@b.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        cognitoUserSyncService.syncUser("x@b.com", "sub10", "usr", "X",
                null, false);

        assertThat(captor.getValue().getRole()).isEqualTo("student");
    }
}
