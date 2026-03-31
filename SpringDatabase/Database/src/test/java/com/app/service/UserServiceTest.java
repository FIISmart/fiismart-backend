package com.app.service;

import com.app.dto.request.UserRequest;
import com.app.dto.response.UserResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Session;
import com.app.model.User;
import com.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private UserService userService;

    private User buildUser(String id, String displayName, String email, String role) {
        return User.builder()
                .id(id)
                .displayName(displayName)
                .email(email)
                .role(role)
                .passwordHash("hashed")
                .banned(false)
                .ownedCourses(new ArrayList<>())
                .enrolledCourseIds(new ArrayList<>())
                .sessions(new ArrayList<>())
                .createdAt(new Date())
                .build();
    }

    @Test
    void create_savesUserAndReturnsResponse() {
        UserRequest request = UserRequest.builder()
                .displayName("Ana Pop")
                .email("ana@test.com")
                .role("student")
                .password("secret123")
                .build();

        User saved = buildUser("id1", "Ana Pop", "ana@test.com", "student");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse response = userService.create(request);

        assertThat(response.getId()).isEqualTo("id1");
        assertThat(response.getDisplayName()).isEqualTo("Ana Pop");
        assertThat(response.getEmail()).isEqualTo("ana@test.com");
        assertThat(response.getRole()).isEqualTo("student");
        assertThat(response.isBanned()).isFalse();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_hashesPassword() {
        UserRequest request = UserRequest.builder()
                .displayName("Dan")
                .email("dan@test.com")
                .role("teacher")
                .password("plaintext")
                .build();

        User saved = buildUser("id2", "Dan", "dan@test.com", "teacher");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        userService.create(request);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("plaintext");
        assertThat(captor.getValue().getPasswordHash()).isNotBlank();
    }

    @Test
    void create_initializesEmptyCollections() {
        UserRequest request = UserRequest.builder()
                .displayName("Admin")
                .email("admin@test.com")
                .role("admin")
                .password("pass")
                .build();

        User saved = buildUser("id3", "Admin", "admin@test.com", "admin");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        userService.create(request);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getOwnedCourses()).isNotNull().isEmpty();
        assertThat(captor.getValue().getEnrolledCourseIds()).isNotNull().isEmpty();
        assertThat(captor.getValue().getSessions()).isNotNull().isEmpty();
        assertThat(captor.getValue().isBanned()).isFalse();
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void findById_returnsUserResponse_whenFound() {
        User user = buildUser("id1", "Ion", "ion@test.com", "student");
        when(userRepository.findById("id1")).thenReturn(Optional.of(user));

        UserResponse response = userService.findById("id1");

        assertThat(response.getId()).isEqualTo("id1");
        assertThat(response.getEmail()).isEqualTo("ion@test.com");
    }

    @Test
    void findById_throwsResourceNotFoundException_whenNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("missing");
    }

    @Test
    void findByEmail_returnsUserResponse_whenFound() {
        User user = buildUser("id1", "Maria", "maria@test.com", "teacher");
        when(userRepository.findByEmail("maria@test.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.findByEmail("maria@test.com");

        assertThat(response.getEmail()).isEqualTo("maria@test.com");
        assertThat(response.getRole()).isEqualTo("teacher");
    }

    @Test
    void findByEmail_throwsResourceNotFoundException_whenNotFound() {
        when(userRepository.findByEmail("nope@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("nope@test.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nope@test.com");
    }

    @Test
    void findAllByRole_returnsListOfResponses() {
        List<User> users = List.of(
                buildUser("u1", "A", "a@t.com", "student"),
                buildUser("u2", "B", "b@t.com", "student")
        );
        when(userRepository.findByRole("student")).thenReturn(users);

        List<UserResponse> responses = userService.findAllByRole("student");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("u1");
        assertThat(responses.get(1).getId()).isEqualTo("u2");
    }

    @Test
    void findAllByRole_returnsEmptyList_whenNoUsersWithRole() {
        when(userRepository.findByRole("admin")).thenReturn(List.of());

        List<UserResponse> responses = userService.findAllByRole("admin");

        assertThat(responses).isEmpty();
    }

    @Test
    void findAllTeachers_callsFindAllByRoleTeacher() {
        when(userRepository.findByRole("teacher")).thenReturn(List.of());

        userService.findAllTeachers();

        verify(userRepository).findByRole("teacher");
    }

    @Test
    void findAllStudents_callsFindAllByRoleStudent() {
        when(userRepository.findByRole("student")).thenReturn(List.of());

        userService.findAllStudents();

        verify(userRepository).findByRole("student");
    }

    @Test
    void findAllAdmins_callsFindAllByRoleAdmin() {
        when(userRepository.findByRole("admin")).thenReturn(List.of());

        userService.findAllAdmins();

        verify(userRepository).findByRole("admin");
    }

    @Test
    void findBannedUsers_returnsBannedUsers() {
        User banned = buildUser("b1", "Banned", "banned@t.com", "student");
        banned.setBanned(true);
        when(userRepository.findByBanned(true)).thenReturn(List.of(banned));

        List<UserResponse> responses = userService.findBannedUsers();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isBanned()).isTrue();
    }

    @Test
    void findBannedUsers_returnsEmptyList_whenNoBannedUsers() {
        when(userRepository.findByBanned(true)).thenReturn(List.of());

        List<UserResponse> responses = userService.findBannedUsers();

        assertThat(responses).isEmpty();
    }

    @Test
    void findStudentsEnrolledInCourse_returnsStudents() {
        User student = buildUser("s1", "Student", "s@t.com", "student");
        when(userRepository.findByRoleAndEnrolledCourseIdsContaining("student", "course1"))
                .thenReturn(List.of(student));

        List<UserResponse> responses = userService.findStudentsEnrolledInCourse("course1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getRole()).isEqualTo("student");
    }

    @Test
    void findStudentsEnrolledInCourse_returnsEmptyList_whenNoneEnrolled() {
        when(userRepository.findByRoleAndEnrolledCourseIdsContaining("student", "course99"))
                .thenReturn(List.of());

        List<UserResponse> responses = userService.findStudentsEnrolledInCourse("course99");

        assertThat(responses).isEmpty();
    }

    @Test
    void updateLastLogin_invokesMongoTemplate() {
        Date now = new Date();

        userService.updateLastLogin("userId1", now);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void updateDisplayName_invokesMongoTemplate() {
        userService.updateDisplayName("userId1", "New Name");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void updatePasswordHash_invokesMongoTemplate() {
        userService.updatePasswordHash("userId1", "newHash");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void banUser_invokesMongoTemplate() {
        userService.banUser("userId1", "adminId", "Violation");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void unbanUser_invokesMongoTemplate() {
        userService.unbanUser("userId1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void addOwnedCourse_invokesMongoTemplate() {
        userService.addOwnedCourse("teacherId", "courseId");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void removeOwnedCourse_invokesMongoTemplate() {
        userService.removeOwnedCourse("teacherId", "courseId");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void addEnrolledCourse_invokesMongoTemplate() {
        userService.addEnrolledCourse("studentId", "courseId");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void removeEnrolledCourse_invokesMongoTemplate() {
        userService.removeEnrolledCourse("studentId", "courseId");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void addSession_invokesMongoTemplate() {
        Session session = Session.builder()
                .token("token123")
                .createdAt(new Date())
                .expiresAt(new Date())
                .build();

        userService.addSession("studentId", session);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void removeSession_invokesMongoTemplate() {
        userService.removeSession("studentId", "token123");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    void deleteById_delegatesToRepository() {
        userService.deleteById("userId1");

        verify(userRepository).deleteById("userId1");
    }

    @Test
    void existsByEmail_returnsTrue_whenExists() {
        when(userRepository.existsByEmail("exists@test.com")).thenReturn(true);

        assertThat(userService.existsByEmail("exists@test.com")).isTrue();
    }

    @Test
    void existsByEmail_returnsFalse_whenNotExists() {
        when(userRepository.existsByEmail("missing@test.com")).thenReturn(false);

        assertThat(userService.existsByEmail("missing@test.com")).isFalse();
    }

    @Test
    void isEnrolledInCourse_returnsTrue_whenEnrolled() {
        when(mongoTemplate.exists(any(Query.class), eq(User.class))).thenReturn(true);

        assertThat(userService.isEnrolledInCourse("studentId", "courseId")).isTrue();
    }

    @Test
    void isEnrolledInCourse_returnsFalse_whenNotEnrolled() {
        when(mongoTemplate.exists(any(Query.class), eq(User.class))).thenReturn(false);

        assertThat(userService.isEnrolledInCourse("studentId", "courseId")).isFalse();
    }

    @Test
    void countByRole_returnsCount() {
        when(userRepository.countByRole("student")).thenReturn(42L);

        assertThat(userService.countByRole("student")).isEqualTo(42L);
    }

    @Test
    void countByRole_returnsZero_whenNoUsersWithRole() {
        when(userRepository.countByRole("admin")).thenReturn(0L);

        assertThat(userService.countByRole("admin")).isEqualTo(0L);
    }

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        Date createdAt = new Date();
        Date lastLoginAt = new Date();
        Date bannedAt = new Date();

        User user = User.builder()
                .id("uid")
                .displayName("Test User")
                .email("test@test.com")
                .role("student")
                .banned(true)
                .bannedBy("adminId")
                .bannedAt(bannedAt)
                .banReason("Spam")
                .ownedCourses(List.of("c1"))
                .enrolledCourseIds(List.of("c2"))
                .createdAt(createdAt)
                .lastLoginAt(lastLoginAt)
                .build();

        when(userRepository.findById("uid")).thenReturn(Optional.of(user));

        UserResponse response = userService.findById("uid");

        assertThat(response.getId()).isEqualTo("uid");
        assertThat(response.getDisplayName()).isEqualTo("Test User");
        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getRole()).isEqualTo("student");
        assertThat(response.isBanned()).isTrue();
        assertThat(response.getBannedBy()).isEqualTo("adminId");
        assertThat(response.getBannedAt()).isEqualTo(bannedAt);
        assertThat(response.getBanReason()).isEqualTo("Spam");
        assertThat(response.getOwnedCourses()).containsExactly("c1");
        assertThat(response.getEnrolledCourseIds()).containsExactly("c2");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getLastLoginAt()).isEqualTo(lastLoginAt);
    }
}
