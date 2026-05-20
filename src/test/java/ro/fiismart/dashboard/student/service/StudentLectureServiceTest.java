package ro.fiismart.dashboard.student.service;

import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.fiismart.common.model.*;
import ro.fiismart.common.repository.*;
import ro.fiismart.dashboard.student.dto.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentLectureServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private ModuleQuizRepository moduleQuizRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private StudentQuizService studentQuizService;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private StudentLectureService studentLectureService;

    private Course sampleCourse;
    private Lecture sampleLecture;
    private CourseModule sampleModule;
    private Enrollment sampleEnrollment;

    @BeforeEach
    void setUp() {
        sampleLecture = Lecture.builder()
                .id("lec1").moduleId("mod1").title("Lecție 1")
                .type("video").videoUrl("https://yt.com/v=abc")
                .order(0).durationSecs(300).build();

        sampleModule = CourseModule.builder()
                .id("mod1").title("Modul 1").order(0)
                .lectures(new ArrayList<>(List.of(sampleLecture))).build();

        sampleCourse = Course.builder()
                .id("course1").title("Matematică")
                .modules(new ArrayList<>(List.of(sampleModule))).build();

        LectureProgressEntry progress = LectureProgressEntry.builder()
                .lectureId("lec1").watchedPercent(50).positionSecs(150)
                .completed(false).updatedAt(new Date()).build();

        sampleEnrollment = Enrollment.builder()
                .id("enroll1").studentId("student1").courseId("course1")
                .status("enrolled").overallProgress(30)
                .lectureProgress(new ArrayList<>(List.of(progress))).build();
    }

    // ── getModules ─────────────────────────────────────────────────────────────

    @Test
    void getModules_courseNotFound_throws() {
        when(courseRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentLectureService.getModules("s1", "noId"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void getModules_returnsMappedModulesWithLectures() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(moduleQuizRepository.findByLectureIdAndQuizScope(any(), any())).thenReturn(Optional.empty());
        when(moduleQuizRepository.findByModuleIdAndQuizScope(any(), any())).thenReturn(Optional.empty());

        List<StudentModuleDTO> result = studentLectureService.getModules("student1", "course1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getModuleId()).isEqualTo("mod1");
        assertThat(result.get(0).getLectures()).hasSize(1);
        assertThat(result.get(0).getLectures().get(0).getLectureId()).isEqualTo("lec1");
    }

    @Test
    void getModules_withProgress_setsProgressOnLecture() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(moduleQuizRepository.findByLectureIdAndQuizScope(any(), any())).thenReturn(Optional.empty());
        when(moduleQuizRepository.findByModuleIdAndQuizScope(any(), any())).thenReturn(Optional.empty());

        List<StudentModuleDTO> result = studentLectureService.getModules("student1", "course1");

        StudentLectureDTO lectureDto = result.get(0).getLectures().get(0);
        assertThat(lectureDto.getWatchedPercent()).isEqualTo(50);
        assertThat(lectureDto.getPositionSecs()).isEqualTo(150);
        assertThat(lectureDto.isCompleted()).isFalse();
    }

    @Test
    void getModules_noEnrollment_returnsModulesWithNoProgress() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.empty());
        when(moduleQuizRepository.findByLectureIdAndQuizScope(any(), any())).thenReturn(Optional.empty());
        when(moduleQuizRepository.findByModuleIdAndQuizScope(any(), any())).thenReturn(Optional.empty());

        List<StudentModuleDTO> result = studentLectureService.getModules("student1", "course1");

        StudentLectureDTO lectureDto = result.get(0).getLectures().get(0);
        assertThat(lectureDto.getWatchedPercent()).isEqualTo(0);
        assertThat(lectureDto.isCompleted()).isFalse();
    }

    @Test
    void getModules_nullModules_returnsEmpty() {
        sampleCourse.setModules(null);
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId(any(), any())).thenReturn(Optional.empty());

        List<StudentModuleDTO> result = studentLectureService.getModules("student1", "course1");

        assertThat(result).isEmpty();
    }

    // ── getLectureDetail ──────────────────────────────────────────────────────

    @Test
    void getLectureDetail_invalidLectureId_throws() {
        assertThatThrownBy(() -> studentLectureService.getLectureDetail("s1", "c1", "undefined"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lecture ID invalid");

        assertThatThrownBy(() -> studentLectureService.getLectureDetail("s1", "c1", null))
                .isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() -> studentLectureService.getLectureDetail("s1", "c1", "  "))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getLectureDetail_courseNotFound_throws() {
        when(courseRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentLectureService.getLectureDetail("s1", "noId", "lec1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getLectureDetail_lectureNotFound_throws() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        assertThatThrownBy(() -> studentLectureService.getLectureDetail("student1", "course1", "noLec"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lecture not found");
    }

    @Test
    void getLectureDetail_found_returnsDTOWithProgress() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(moduleQuizRepository.findByLectureIdAndQuizScope("lec1", "lecture"))
                .thenReturn(Optional.empty());

        StudentLectureDetailDTO result = studentLectureService.getLectureDetail("student1", "course1", "lec1");

        assertThat(result.getLectureId()).isEqualTo("lec1");
        assertThat(result.getTitle()).isEqualTo("Lecție 1");
        assertThat(result.getWatchedPercent()).isEqualTo(50);
        assertThat(result.getPositionSecs()).isEqualTo(150);
    }

    // ── updateLectureProgress ─────────────────────────────────────────────────

    @Test
    void updateLectureProgress_invalidLectureId_throws() {
        assertThatThrownBy(() -> studentLectureService.updateLectureProgress(
                "s1", "c1", "undefined", new StudentLectureProgressRequest()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateLectureProgress_studentNotEnrolled_throws() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("s1", "course1"))
                .thenReturn(Optional.empty());

        StudentLectureProgressRequest req = new StudentLectureProgressRequest();
        req.setWatchedPercent(50);
        req.setPositionSecs(150);

        assertThatThrownBy(() -> studentLectureService.updateLectureProgress("s1", "course1", "lec1", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not enrolled");
    }

    @Test
    void updateLectureProgress_validRequest_updatesProgress() {
        UpdateResult matchedResult = mock(UpdateResult.class);
        when(matchedResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(), any(), eq(Enrollment.class))).thenReturn(matchedResult);

        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(enrollmentRepository.findById("enroll1")).thenReturn(Optional.of(sampleEnrollment));
        when(moduleQuizRepository.findAllByCourseId("course1")).thenReturn(List.of());

        StudentLectureProgressRequest req = new StudentLectureProgressRequest();
        req.setWatchedPercent(75);
        req.setPositionSecs(225);
        req.setCompleted(false);

        StudentLectureProgressResponse result = studentLectureService.updateLectureProgress(
                "student1", "course1", "lec1", req);

        assertThat(result).isNotNull();
        assertThat(result.getLectureId()).isEqualTo("lec1");
        assertThat(result.getWatchedPercent()).isGreaterThanOrEqualTo(50);
    }

    @Test
    void updateLectureProgress_watchedPercent95_setsCompleted() {
        UpdateResult matchedResult = mock(UpdateResult.class);
        when(matchedResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(), any(), eq(Enrollment.class))).thenReturn(matchedResult);

        LectureProgressEntry noProgress = LectureProgressEntry.builder()
                .lectureId("lec1").watchedPercent(0).completed(false).updatedAt(new Date()).build();
        sampleEnrollment.setLectureProgress(new ArrayList<>(List.of(noProgress)));

        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(enrollmentRepository.findById("enroll1")).thenReturn(Optional.of(sampleEnrollment));
        when(moduleQuizRepository.findAllByCourseId("course1")).thenReturn(List.of());

        StudentLectureProgressRequest req = new StudentLectureProgressRequest();
        req.setWatchedPercent(96);
        req.setPositionSecs(290);

        StudentLectureProgressResponse result = studentLectureService.updateLectureProgress(
                "student1", "course1", "lec1", req);

        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void updateLectureProgress_overallProgress100_marksCourseCompleted() {
        UpdateResult matchedResult = mock(UpdateResult.class);
        when(matchedResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(), any(), eq(Enrollment.class))).thenReturn(matchedResult);

        // Mark existing progress as completed
        LectureProgressEntry completedProgress = LectureProgressEntry.builder()
                .lectureId("lec1").watchedPercent(100).completed(true).updatedAt(new Date()).build();
        sampleEnrollment.setLectureProgress(new ArrayList<>(List.of(completedProgress)));

        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(enrollmentRepository.findById("enroll1")).thenReturn(Optional.of(sampleEnrollment));
        when(moduleQuizRepository.findAllByCourseId("course1")).thenReturn(List.of());

        StudentLectureProgressRequest req = new StudentLectureProgressRequest();
        req.setWatchedPercent(100);
        req.setPositionSecs(300);
        req.setCompleted(true);

        StudentLectureProgressResponse result = studentLectureService.updateLectureProgress(
                "student1", "course1", "lec1", req);

        assertThat(result.isCourseCompleted()).isTrue();
        assertThat(result.getOverallProgress()).isEqualTo(100);
    }

    @Test
    void updateLectureProgress_clampsPositionToMax() {
        UpdateResult matchedResult = mock(UpdateResult.class);
        when(matchedResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(), any(), eq(Enrollment.class))).thenReturn(matchedResult);

        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(enrollmentRepository.findById("enroll1")).thenReturn(Optional.of(sampleEnrollment));
        when(moduleQuizRepository.findAllByCourseId("course1")).thenReturn(List.of());

        StudentLectureProgressRequest req = new StudentLectureProgressRequest();
        req.setWatchedPercent(80);
        req.setPositionSecs(9999); // too high

        StudentLectureProgressResponse result = studentLectureService.updateLectureProgress(
                "student1", "course1", "lec1", req);

        assertThat(result.getPositionSecs()).isEqualTo(300); // clamped to durationSecs
    }
}
