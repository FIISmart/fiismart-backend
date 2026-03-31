package com.app.service;

import com.app.dto.request.EnrollmentRequest;
import com.app.dto.response.EnrollmentResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Enrollment;
import com.app.model.LectureProgress;
import com.app.repository.EnrollmentRepository;
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
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Enrollment buildEnrollment(String id, String studentId, String courseId) {
        return Enrollment.builder()
                .id(id)
                .studentId(studentId)
                .courseId(courseId)
                .enrolledAt(new Date())
                .status("enrolled")
                .overallProgress(0)
                .lectureProgress(new ArrayList<>())
                .build();
    }

    @Test
    void create_savesEnrollmentAndReturnsResponse() {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .studentId("s1")
                .courseId("c1")
                .build();

        Enrollment saved = buildEnrollment("e1", "s1", "c1");
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(saved);

        EnrollmentResponse response = enrollmentService.create(request);

        assertThat(response.getId()).isEqualTo("e1");
        assertThat(response.getStudentId()).isEqualTo("s1");
        assertThat(response.getCourseId()).isEqualTo("c1");
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void create_setsInitialValues() {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .studentId("s1")
                .courseId("c1")
                .build();

        Enrollment saved = buildEnrollment("e1", "s1", "c1");
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(saved);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        enrollmentService.create(request);
        verify(enrollmentRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo("enrolled");
        assertThat(captor.getValue().getOverallProgress()).isEqualTo(0);
        assertThat(captor.getValue().getLectureProgress()).isNotNull().isEmpty();
        assertThat(captor.getValue().getEnrolledAt()).isNotNull();
    }

    @Test
    void findById_returnsResponse_whenFound() {
        Enrollment enrollment = buildEnrollment("e1", "s1", "c1");
        when(enrollmentRepository.findById("e1")).thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = enrollmentService.findById("e1");

        assertThat(response.getId()).isEqualTo("e1");
    }

    @Test
    void findById_throwsResourceNotFoundException_whenNotFound() {
        when(enrollmentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Enrollment")
                .hasMessageContaining("missing");
    }

    @Test
    void findByStudentAndCourse_returnsResponse_whenFound() {
        Enrollment enrollment = buildEnrollment("e1", "s1", "c1");
        when(enrollmentRepository.findByStudentIdAndCourseId("s1", "c1"))
                .thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = enrollmentService.findByStudentAndCourse("s1", "c1");

        assertThat(response.getStudentId()).isEqualTo("s1");
        assertThat(response.getCourseId()).isEqualTo("c1");
    }

    @Test
    void findByStudentAndCourse_throwsResourceNotFoundException_whenNotFound() {
        when(enrollmentRepository.findByStudentIdAndCourseId("s1", "c1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.findByStudentAndCourse("s1", "c1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("s1")
                .hasMessageContaining("c1");
    }

    @Test
    void findByStudentId_returnsList() {
        List<Enrollment> enrollments = List.of(
                buildEnrollment("e1", "s1", "c1"),
                buildEnrollment("e2", "s1", "c2")
        );
        when(enrollmentRepository.findByStudentId("s1")).thenReturn(enrollments);

        List<EnrollmentResponse> responses = enrollmentService.findByStudentId("s1");

        assertThat(responses).hasSize(2);
    }

    @Test
    void findByStudentId_returnsEmptyList_whenNone() {
        when(enrollmentRepository.findByStudentId("s99")).thenReturn(List.of());

        assertThat(enrollmentService.findByStudentId("s99")).isEmpty();
    }

    @Test
    void findByCourseId_returnsList() {
        List<Enrollment> enrollments = List.of(
                buildEnrollment("e1", "s1", "c1"),
                buildEnrollment("e2", "s2", "c1")
        );
        when(enrollmentRepository.findByCourseId("c1")).thenReturn(enrollments);

        List<EnrollmentResponse> responses = enrollmentService.findByCourseId("c1");

        assertThat(responses).hasSize(2);
    }

    @Test
    void findCompletedByStudent_returnsCompletedEnrollments() {
        Enrollment completed = buildEnrollment("e1", "s1", "c1");
        completed.setStatus("completed");
        when(enrollmentRepository.findByStudentIdAndStatus("s1", "completed"))
                .thenReturn(List.of(completed));

        List<EnrollmentResponse> responses = enrollmentService.findCompletedByStudent("s1");

        assertThat(responses).hasSize(1);
        verify(enrollmentRepository).findByStudentIdAndStatus("s1", "completed");
    }

    @Test
    void findCompletedByStudent_returnsEmptyList_whenNone() {
        when(enrollmentRepository.findByStudentIdAndStatus("s1", "completed"))
                .thenReturn(List.of());

        assertThat(enrollmentService.findCompletedByStudent("s1")).isEmpty();
    }

    @Test
    void updateStatus_invokesMongoTemplate() {
        enrollmentService.updateStatus("e1", "completed");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Enrollment.class));
    }

    @Test
    void updateOverallProgress_invokesMongoTemplate() {
        enrollmentService.updateOverallProgress("e1", 75);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Enrollment.class));
    }

    @Test
    void updateLastAccessedAt_invokesMongoTemplate() {
        enrollmentService.updateLastAccessedAt("e1", new Date());

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Enrollment.class));
    }

    @Test
    void markCompleted_invokesMongoTemplate() {
        enrollmentService.markCompleted("e1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Enrollment.class));
    }

    @Test
    void addLectureProgress_invokesMongoTemplate() {
        LectureProgress progress = LectureProgress.builder()
                .lectureId("lec1")
                .watchedSecs(120)
                .completed(false)
                .lastWatchedAt(new Date())
                .build();

        enrollmentService.addLectureProgress("e1", progress);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Enrollment.class));
    }

    @Test
    void updateLectureProgressField_invokesMongoTemplate() {
        enrollmentService.updateLectureProgressField("e1", "lec1", "watchedSecs", 300);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Enrollment.class));
    }

    @Test
    void removeLectureProgress_invokesMongoTemplate() {
        enrollmentService.removeLectureProgress("e1", "lec1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Enrollment.class));
    }

    @Test
    void deleteById_delegatesToRepository() {
        enrollmentService.deleteById("e1");

        verify(enrollmentRepository).deleteById("e1");
    }

    @Test
    void deleteByStudentAndCourse_delegatesToRepository() {
        enrollmentService.deleteByStudentAndCourse("s1", "c1");

        verify(enrollmentRepository).deleteByStudentIdAndCourseId("s1", "c1");
    }

    @Test
    void deleteAllByCourse_delegatesToRepository() {
        enrollmentService.deleteAllByCourse("c1");

        verify(enrollmentRepository).deleteByCourseId("c1");
    }

    @Test
    void isEnrolled_returnsTrue_whenEnrolled() {
        when(enrollmentRepository.existsByStudentIdAndCourseId("s1", "c1")).thenReturn(true);

        assertThat(enrollmentService.isEnrolled("s1", "c1")).isTrue();
    }

    @Test
    void isEnrolled_returnsFalse_whenNotEnrolled() {
        when(enrollmentRepository.existsByStudentIdAndCourseId("s1", "c1")).thenReturn(false);

        assertThat(enrollmentService.isEnrolled("s1", "c1")).isFalse();
    }

    @Test
    void countByCourse_returnsCount() {
        when(enrollmentRepository.countByCourseId("c1")).thenReturn(15L);

        assertThat(enrollmentService.countByCourse("c1")).isEqualTo(15L);
    }

    @Test
    void countCompletedByCourse_returnsCount() {
        when(enrollmentRepository.countByCourseIdAndStatus("c1", "completed")).thenReturn(8L);

        assertThat(enrollmentService.countCompletedByCourse("c1")).isEqualTo(8L);
    }

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        Date enrolledAt = new Date();
        Date completedAt = new Date();
        Date lastAccessedAt = new Date();

        LectureProgress lp = LectureProgress.builder()
                .lectureId("lec1")
                .watchedSecs(200)
                .completed(true)
                .lastWatchedAt(new Date())
                .build();

        Enrollment enrollment = Enrollment.builder()
                .id("e1")
                .studentId("s1")
                .courseId("c1")
                .enrolledAt(enrolledAt)
                .completedAt(completedAt)
                .status("completed")
                .lectureProgress(List.of(lp))
                .lastAccessedAt(lastAccessedAt)
                .overallProgress(100)
                .build();

        when(enrollmentRepository.findById("e1")).thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = enrollmentService.findById("e1");

        assertThat(response.getId()).isEqualTo("e1");
        assertThat(response.getStudentId()).isEqualTo("s1");
        assertThat(response.getCourseId()).isEqualTo("c1");
        assertThat(response.getEnrolledAt()).isEqualTo(enrolledAt);
        assertThat(response.getCompletedAt()).isEqualTo(completedAt);
        assertThat(response.getStatus()).isEqualTo("completed");
        assertThat(response.getLectureProgress()).hasSize(1);
        assertThat(response.getLastAccessedAt()).isEqualTo(lastAccessedAt);
        assertThat(response.getOverallProgress()).isEqualTo(100);
    }
}
