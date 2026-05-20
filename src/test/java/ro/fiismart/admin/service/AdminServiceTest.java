package ro.fiismart.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import ro.fiismart.admin.dto.*;
import ro.fiismart.auth.service.CognitoAdminService;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CognitoAdminService cognitoAdminService;

    @InjectMocks
    private AdminService adminService;

    private User sampleUser;
    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("user1")
                .email("test@test.com")
                .displayName("Ion Popescu")
                .role("student")
                .cognitoUsername("test@test.com")
                .banned(false)
                .createdAt(new Date())
                .build();

        sampleCourse = Course.builder()
                .id("course1")
                .title("Matematică")
                .teacherId("teacher1")
                .status("published")
                .hidden(false)
                .build();
    }

    // ── getStats ──────────────────────────────────────────────────────────────

    @Test
    void getStats_returnsAggregatedStats() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByRole("student")).thenReturn(80L);
        when(userRepository.countByRole("professor")).thenReturn(15L);
        when(userRepository.countByRole("admin")).thenReturn(5L);
        when(courseRepository.count()).thenReturn(20L);
        when(courseRepository.findByStatusAndHiddenFalse("published")).thenReturn(List.of(sampleCourse));
        when(enrollmentRepository.count()).thenReturn(200L);
        when(commentRepository.count()).thenReturn(50L);

        AdminStatsResponse stats = adminService.getStats();

        assertThat(stats.getTotalUsers()).isEqualTo(100L);
        assertThat(stats.getTotalStudents()).isEqualTo(80L);
        assertThat(stats.getTotalProfessors()).isEqualTo(15L);
        assertThat(stats.getTotalAdmins()).isEqualTo(5L);
        assertThat(stats.getTotalCourses()).isEqualTo(20L);
        assertThat(stats.getPublishedCourses()).isEqualTo(1L);
        assertThat(stats.getTotalEnrollments()).isEqualTo(200L);
        assertThat(stats.getTotalComments()).isEqualTo(50L);
    }

    // ── getAllUsers ───────────────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsAllMappedToDTO() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<AdminUserResponse> result = adminService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("user1");
        assertThat(result.get(0).getEmail()).isEqualTo("test@test.com");
        assertThat(result.get(0).isAdmin()).isFalse();
    }

    // ── updateUser ───────────────────────────────────────────────────────────

    @Test
    void updateUser_updatesDisplayName() {
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setDisplayName("  Gheorghe Ionescu  ");

        AdminUserResponse result = adminService.updateUser("user1", req, "admin99");

        assertThat(sampleUser.getDisplayName()).isEqualTo("Gheorghe Ionescu");
        verify(userRepository).save(sampleUser);
    }

    @Test
    void updateUser_promotesToAdmin_addsToGroup() {
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setIsAdmin(true);

        adminService.updateUser("user1", req, "admin99");

        assertThat(sampleUser.getRole()).isEqualTo("admin");
        assertThat(sampleUser.getBaseRole()).isEqualTo("student");
        verify(cognitoAdminService).addUserToGroup("test@test.com", "ADMIN");
    }

    @Test
    void updateUser_promoteToAdmin_alreadyAdmin_doesNothing() {
        sampleUser.setRole("admin");
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setIsAdmin(true);

        adminService.updateUser("user1", req, "admin99");

        verify(cognitoAdminService, never()).addUserToGroup(any(), any());
    }

    @Test
    void updateUser_demoteFromAdmin_restoresBaseRole() {
        sampleUser.setRole("admin");
        sampleUser.setBaseRole("teacher");
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setIsAdmin(false);

        adminService.updateUser("user1", req, "admin99");

        assertThat(sampleUser.getRole()).isEqualTo("teacher");
        assertThat(sampleUser.getBaseRole()).isNull();
        verify(cognitoAdminService).removeUserFromGroup("test@test.com", "ADMIN");
    }

    @Test
    void updateUser_demoteFromAdmin_noBaseRole_restoresToStudent() {
        sampleUser.setRole("admin");
        sampleUser.setBaseRole(null);
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setIsAdmin(false);

        adminService.updateUser("user1", req, "admin99");

        assertThat(sampleUser.getRole()).isEqualTo("student");
    }

    @Test
    void updateUser_demoteWhenNotAdmin_doesNothing() {
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setIsAdmin(false);

        adminService.updateUser("user1", req, "admin99");

        verify(cognitoAdminService, never()).removeUserFromGroup(any(), any());
    }

    @Test
    void updateUser_bansUser_setsBanFields() {
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setBanned(true);
        req.setBanReason("Comportament neadecvat");

        adminService.updateUser("user1", req, "admin99");

        assertThat(sampleUser.isBanned()).isTrue();
        assertThat(sampleUser.getBanReason()).isEqualTo("Comportament neadecvat");
        assertThat(sampleUser.getBannedAt()).isNotNull();
    }

    @Test
    void updateUser_unbansUser_clearsBanFields() {
        sampleUser.setBanned(true);
        sampleUser.setBanReason("Spam");
        sampleUser.setBannedAt(new Date());
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setBanned(false);

        adminService.updateUser("user1", req, "admin99");

        assertThat(sampleUser.isBanned()).isFalse();
        assertThat(sampleUser.getBanReason()).isNull();
        assertThat(sampleUser.getBannedAt()).isNull();
    }

    @Test
    void updateUser_notFound_throwsResponseStatusException() {
        when(userRepository.findById("notExist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUser("notExist", new AdminUpdateUserRequest(), "admin99"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateUser_emptyDisplayName_doesNotUpdate() {
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setDisplayName("   ");

        adminService.updateUser("user1", req, "admin99");

        assertThat(sampleUser.getDisplayName()).isEqualTo("Ion Popescu");
    }

    @Test
    void updateUser_usesEmailIfNoCognitoUsername() {
        sampleUser.setCognitoUsername(null);
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setIsAdmin(true);

        adminService.updateUser("user1", req, "admin99");

        verify(cognitoAdminService).addUserToGroup("test@test.com", "ADMIN");
    }

    @Test
    void updateUser_selfBan_throwsForbidden() {
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setBanned(true);
        req.setBanReason("Test");

        assertThatThrownBy(() -> adminService.updateUser("user1", req, "user1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void updateUser_selfRoleChange_throwsForbidden() {
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setIsAdmin(false);

        assertThatThrownBy(() -> adminService.updateUser("user1", req, "user1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(403));
    }

    // ── deleteUser ───────────────────────────────────────────────────────────

    @Test
    void deleteUser_deletesFromCognitoAndDB() {
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));

        adminService.deleteUser("user1");

        verify(cognitoAdminService).deleteUser("test@test.com");
        verify(userRepository).deleteById("user1");
    }

    @Test
    void deleteUser_noCognitoUsername_doesNotCallCognito() {
        sampleUser.setCognitoUsername(null);
        sampleUser.setEmail(null);
        when(userRepository.findById("user1")).thenReturn(Optional.of(sampleUser));

        adminService.deleteUser("user1");

        verify(cognitoAdminService, never()).deleteUser(any());
        verify(userRepository).deleteById("user1");
    }

    @Test
    void deleteUser_notFound_throwsResponseStatusException() {
        when(userRepository.findById("notExist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteUser("notExist"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ── getAllCourses / updateCourse / deleteCourse ───────────────────────────

    @Test
    void getAllCourses_returnsAllMapped() {
        when(courseRepository.findAll()).thenReturn(List.of(sampleCourse));

        List<AdminCourseResponse> result = adminService.getAllCourses();

        assertThat(result).hasSize(1);
    }

    @Test
    void updateCourse_updatesFields() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        AdminUpdateCourseRequest req = new AdminUpdateCourseRequest();
        req.setTitle("Matematică Avansată");
        req.setHidden(true);
        req.setStatus("draft");

        AdminCourseResponse result = adminService.updateCourse("course1", req);

        assertThat(sampleCourse.getTitle()).isEqualTo("Matematică Avansată");
        assertThat(sampleCourse.isHidden()).isTrue();
        assertThat(sampleCourse.getStatus()).isEqualTo("draft");
    }

    @Test
    void updateCourse_notFound_throws() {
        when(courseRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateCourse("noId", new AdminUpdateCourseRequest()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateCourse_blankTitle_doesNotUpdate() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        AdminUpdateCourseRequest req = new AdminUpdateCourseRequest();
        req.setTitle("   ");

        adminService.updateCourse("course1", req);

        assertThat(sampleCourse.getTitle()).isEqualTo("Matematică");
    }

    @Test
    void deleteCourse_deletesAndCleansEnrollments() {
        when(courseRepository.existsById("course1")).thenReturn(true);

        adminService.deleteCourse("course1");

        verify(courseRepository).deleteById("course1");
        verify(enrollmentRepository).deleteByCourseId("course1");
    }

    @Test
    void deleteCourse_notFound_throws() {
        when(courseRepository.existsById("noId")).thenReturn(false);

        assertThatThrownBy(() -> adminService.deleteCourse("noId"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ── enrollments ──────────────────────────────────────────────────────────

    @Test
    void getAllEnrollments_returnsList() {
        when(enrollmentRepository.findAll()).thenReturn(List.of());

        List<AdminEnrollmentResponse> result = adminService.getAllEnrollments();

        assertThat(result).isEmpty();
    }

    @Test
    void deleteEnrollment_deletesIfExists() {
        when(enrollmentRepository.existsById("e1")).thenReturn(true);

        adminService.deleteEnrollment("e1");

        verify(enrollmentRepository).deleteById("e1");
    }

    @Test
    void deleteEnrollment_notFound_throws() {
        when(enrollmentRepository.existsById("noId")).thenReturn(false);

        assertThatThrownBy(() -> adminService.deleteEnrollment("noId"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
