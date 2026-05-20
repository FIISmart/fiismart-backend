package ro.fiismart.dashboard.student.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.dashboard.student.dto.InitialsDTO;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentInitialsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudentInitialsService studentInitialsService;

    @Test
    void getInitials_twoWordName_returnsTwoInitials() {
        User user = User.builder().id("s1").displayName("Ion Popescu").build();
        when(userRepository.findById("s1")).thenReturn(Optional.of(user));

        InitialsDTO result = studentInitialsService.getInitials("s1");

        assertThat(result.getInitials()).isEqualTo("IP");
    }

    @Test
    void getInitials_oneWordName_returnsOneInitial() {
        User user = User.builder().id("s1").displayName("Madonna").build();
        when(userRepository.findById("s1")).thenReturn(Optional.of(user));

        InitialsDTO result = studentInitialsService.getInitials("s1");

        assertThat(result.getInitials()).isEqualTo("M");
    }

    @Test
    void getInitials_nullDisplayName_returnsEmpty() {
        User user = User.builder().id("s1").displayName(null).build();
        when(userRepository.findById("s1")).thenReturn(Optional.of(user));

        InitialsDTO result = studentInitialsService.getInitials("s1");

        assertThat(result.getInitials()).isEqualTo("");
    }

    @Test
    void getInitials_blankDisplayName_returnsEmpty() {
        User user = User.builder().id("s1").displayName("   ").build();
        when(userRepository.findById("s1")).thenReturn(Optional.of(user));

        InitialsDTO result = studentInitialsService.getInitials("s1");

        assertThat(result.getInitials()).isEqualTo("");
    }

    @Test
    void getInitials_userNotFound_returnsNull() {
        when(userRepository.findById("noId")).thenReturn(Optional.empty());

        InitialsDTO result = studentInitialsService.getInitials("noId");

        assertThat(result).isNull();
    }

    @Test
    void getInitials_threeWordName_usesFirstTwoInitials() {
        User user = User.builder().id("s1").displayName("Ion Alexandru Popescu").build();
        when(userRepository.findById("s1")).thenReturn(Optional.of(user));

        InitialsDTO result = studentInitialsService.getInitials("s1");

        assertThat(result.getInitials()).isEqualTo("IA");
    }

    @Test
    void getInitials_lowercaseName_returnsUppercase() {
        User user = User.builder().id("s1").displayName("ion popescu").build();
        when(userRepository.findById("s1")).thenReturn(Optional.of(user));

        InitialsDTO result = studentInitialsService.getInitials("s1");

        assertThat(result.getInitials()).isEqualTo("IP");
    }
}
