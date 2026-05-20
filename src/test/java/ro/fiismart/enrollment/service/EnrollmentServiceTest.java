package ro.fiismart.enrollment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.LectureProgressEntry;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.enrollment.dto.EnrollmentRequest;
import ro.fiismart.enrollment.dto.EnrollmentResponse;
import ro.fiismart.notification.service.NotificationService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Enrollment sampleEnrollment;
    private Course sampleCourse;
    private User sampleStudent;

    @BeforeEach
    void setUp() {
        sampleEnrollment = Enrollment.builder()
                .id("enroll1")
                .studentId("student1")
                .courseId("course1")
                .enrolledAt(new Date())
                .status("enrolled")
                .overallProgress(0)
                .lectureProgress(new ArrayList<>())
                .build();

        sampleCourse = Course.builder()
                .id("course1")
                .title("Matematică")
                .teacherId("teacher1")
                .build();

        sampleStudent = User.builder()
                .id("student1")
                .displayName("Ion Popescu")
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_newEnrollment_savesAndSendsNotifications() {
        EnrollmentRequest req = new EnrollmentRequest();
        req.setStudentId("student1");
        req.setCourseId("course1");

        when(enrollmentRepository.existsByStudentIdAndCourseId("student1", "course1")).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(sampleEnrollment);
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleStudent));

        EnrollmentResponse result = enrollmentService.create(req);

        assertThat(result.getId()).isEqualTo("enroll1");
        verify(notificationService).createStudentEnrollmentNotification("student1", "Matematică", "course1");
        verify(notificationService).createEnrollmentNotification("teacher1", "Ion Popescu", "Matematică", "course1");
    }

    @Test
    void create_existingEnrollment_returnsExisting() {
        EnrollmentRequest req = new EnrollmentRequest();
        req.setStudentId("student1");
        req.setCourseId("course1");

        when(enrollmentRepository.existsByStudentIdAndCourseId("student1", "course1")).thenReturn(true);
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));

        EnrollmentResponse result = enrollmentService.create(req);

        assertThat(result.getId()).isEqualTo("enroll1");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void create_notificationExceptionDoesNotBreakFlow() {
        EnrollmentRequest req = new EnrollmentRequest();
        req.setStudentId("student1");
        req.setCourseId("course1");

        when(enrollmentRepository.existsByStudentIdAndCourseId("student1", "course1")).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(sampleEnrollment);
        when(courseRepository.findById("course1")).thenThrow(new RuntimeException("DB error"));

        EnrollmentResponse result = enrollmentService.create(req);

        assertThat(result).isNotNull();
    }

    @Test
    void create_courseWithNoTeacher_doesNotNotifyTeacher() {
        sampleCourse.setTeacherId(null);
        EnrollmentRequest req = new EnrollmentRequest();
        req.setStudentId("student1");
        req.setCourseId("course1");

        when(enrollmentRepository.existsByStudentIdAndCourseId("student1", "course1")).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(sampleEnrollment);
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleStudent));

        enrollmentService.create(req);

        verify(notificationService, never()).createEnrollmentNotification(any(), any(), any(), any());
    }

    @Test
    void create_studentNotFound_usesDefaultName() {
        EnrollmentRequest req = new EnrollmentRequest();
        req.setStudentId("student1");
        req.setCourseId("course1");

        when(enrollmentRepository.existsByStudentIdAndCourseId("student1", "course1")).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(sampleEnrollment);
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(userRepository.findById("student1")).thenReturn(Optional.empty());

        enrollmentService.create(req);

        verify(notificationService).createEnrollmentNotification("teacher1", "Un student", "Matematică", "course1");
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_found_returnsResponse() {
        when(enrollmentRepository.findById("enroll1")).thenReturn(Optional.of(sampleEnrollment));

        EnrollmentResponse result = enrollmentService.findById("enroll1");

        assertThat(result.getId()).isEqualTo("enroll1");
    }

    @Test
    void findById_notFound_throws() {
        when(enrollmentRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.findById("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── findByStudentAndCourse ────────────────────────────────────────────────

    @Test
    void findByStudentAndCourse_found_returnsResponse() {
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));

        EnrollmentResponse result = enrollmentService.findByStudentAndCourse("student1", "course1");

        assertThat(result.getStudentId()).isEqualTo("student1");
    }

    @Test
    void findByStudentAndCourse_notFound_throws() {
        when(enrollmentRepository.findByStudentIdAndCourseId("s", "c")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.findByStudentAndCourse("s", "c"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── findByStudentId / findByCourseId / findCompleted ─────────────────────

    @Test
    void findByStudentId_returnsMappedList() {
        when(enrollmentRepository.findByStudentId("student1")).thenReturn(List.of(sampleEnrollment));

        List<EnrollmentResponse> result = enrollmentService.findByStudentId("student1");

        assertThat(result).hasSize(1);
    }

    @Test
    void findByCourseId_returnsMappedList() {
        when(enrollmentRepository.findByCourseId("course1")).thenReturn(List.of(sampleEnrollment));

        List<EnrollmentResponse> result = enrollmentService.findByCourseId("course1");

        assertThat(result).hasSize(1);
    }

    @Test
    void findCompletedByStudent_returnsMappedList() {
        sampleEnrollment.setStatus("completed");
        when(enrollmentRepository.findByStudentIdAndStatus("student1", "completed"))
                .thenReturn(List.of(sampleEnrollment));

        List<EnrollmentResponse> result = enrollmentService.findCompletedByStudent("student1");

        assertThat(result).hasSize(1);
    }

    // ── update methods ────────────────────────────────────────────────────────

    @Test
    void updateStatus_callsMongoTemplate() {
        enrollmentService.updateStatus("enroll1", "completed");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Enrollment.class));
    }

    @Test
    void updateOverallProgress_callsMongoTemplate() {
        enrollmentService.updateOverallProgress("enroll1", 75);

        verify(mongoTemplate).updateFirst(any(), any(), eq(Enrollment.class));
    }

    @Test
    void markCompleted_callsMongoTemplate() {
        enrollmentService.markCompleted("enroll1");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Enrollment.class));
    }

    @Test
    void addLectureProgress_callsMongoTemplate() {
        LectureProgressEntry entry = new LectureProgressEntry();
        enrollmentService.addLectureProgress("enroll1", entry);

        verify(mongoTemplate).updateFirst(any(), any(), eq(Enrollment.class));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteById_callsRepository() {
        enrollmentService.deleteById("enroll1");

        verify(enrollmentRepository).deleteById("enroll1");
    }

    @Test
    void deleteByStudentAndCourse_callsRepository() {
        enrollmentService.deleteByStudentAndCourse("student1", "course1");

        verify(enrollmentRepository).deleteByStudentIdAndCourseId("student1", "course1");
    }

    // ── isEnrolled / countByCourse ────────────────────────────────────────────

    @Test
    void isEnrolled_delegatesToRepository() {
        when(enrollmentRepository.existsByStudentIdAndCourseId("s", "c")).thenReturn(true);

        assertThat(enrollmentService.isEnrolled("s", "c")).isTrue();
    }

    @Test
    void countByCourse_delegatesToRepository() {
        when(enrollmentRepository.countByCourseId("course1")).thenReturn(42L);

        assertThat(enrollmentService.countByCourse("course1")).isEqualTo(42L);
    }
}
