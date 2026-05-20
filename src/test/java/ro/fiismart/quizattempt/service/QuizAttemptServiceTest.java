package ro.fiismart.quizattempt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.*;
import ro.fiismart.common.repository.*;
import ro.fiismart.quizattempt.dto.QuizAttemptRequest;
import ro.fiismart.quizattempt.dto.QuizAttemptResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ModuleQuizRepository moduleQuizRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private QuizAttemptService quizAttemptService;

    private QuizAttempt sampleAttempt;
    private Course sampleCourse;
    private Enrollment sampleEnrollment;

    @BeforeEach
    void setUp() {
        sampleAttempt = QuizAttempt.builder()
                .id("attempt1")
                .quizId("quiz1")
                .courseId("course1")
                .studentId("student1")
                .attemptedAt(new Date())
                .score(85)
                .passed(true)
                .timeTakenSecs(300)
                .answers(new ArrayList<>())
                .build();

        Lecture lecture = Lecture.builder().id("lec1").build();
        CourseModule module = CourseModule.builder()
                .id("mod1")
                .lectures(new ArrayList<>(List.of(lecture)))
                .build();

        sampleCourse = Course.builder()
                .id("course1")
                .modules(new ArrayList<>(List.of(module)))
                .quizId("quiz1")
                .build();

        LectureProgressEntry progressEntry = new LectureProgressEntry();
        progressEntry.setLectureId("lec1");
        progressEntry.setCompleted(true);

        sampleEnrollment = Enrollment.builder()
                .id("enroll1")
                .studentId("student1")
                .courseId("course1")
                .lectureProgress(new ArrayList<>(List.of(progressEntry)))
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_savesAttemptAndRefreshesProgress() {
        QuizAttemptRequest req = new QuizAttemptRequest();
        req.setQuizId("quiz1");
        req.setCourseId("course1");
        req.setStudentId("student1");
        req.setScore(90);
        req.setPassed(true);
        req.setTimeTakenSecs(200);

        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(sampleAttempt);
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(moduleQuizRepository.findAllByCourseId("course1")).thenReturn(List.of());
        when(quizAttemptRepository.findByStudentIdAndQuizId(eq("student1"), any()))
                .thenReturn(List.of(sampleAttempt));

        QuizAttemptResponse result = quizAttemptService.create(req);

        assertThat(result.getId()).isEqualTo("attempt1");
        assertThat(result.getScore()).isEqualTo(85);
        verify(mongoTemplate).updateFirst(any(), any(), eq(Enrollment.class));
    }

    @Test
    void create_nullStudentId_doesNotRefresh() {
        QuizAttemptRequest req = new QuizAttemptRequest();
        req.setStudentId(null);
        req.setCourseId("course1");

        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(sampleAttempt);

        QuizAttemptResponse result = quizAttemptService.create(req);

        assertThat(result).isNotNull();
        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(Enrollment.class));
    }

    @Test
    void create_courseNotFound_doesNotRefresh() {
        QuizAttemptRequest req = new QuizAttemptRequest();
        req.setStudentId("student1");
        req.setCourseId("course1");

        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(sampleAttempt);
        when(courseRepository.findById("course1")).thenReturn(Optional.empty());

        quizAttemptService.create(req);

        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(Enrollment.class));
    }

    @Test
    void create_withNullAnswers_usesEmptyList() {
        QuizAttemptRequest req = new QuizAttemptRequest();
        req.setStudentId("student1");
        req.setCourseId("course1");
        req.setAnswers(null);

        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(sampleAttempt);
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(moduleQuizRepository.findAllByCourseId("course1")).thenReturn(List.of());
        when(quizAttemptRepository.findByStudentIdAndQuizId(any(), any())).thenReturn(List.of());

        quizAttemptService.create(req);

        verify(quizAttemptRepository).save(any());
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_found_returnsResponse() {
        when(quizAttemptRepository.findById("attempt1")).thenReturn(Optional.of(sampleAttempt));

        QuizAttemptResponse result = quizAttemptService.findById("attempt1");

        assertThat(result.getId()).isEqualTo("attempt1");
    }

    @Test
    void findById_notFound_throws() {
        when(quizAttemptRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizAttemptService.findById("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── findBy... ─────────────────────────────────────────────────────────────

    @Test
    void findByStudentId_returnsMappedList() {
        when(quizAttemptRepository.findByStudentId("student1")).thenReturn(List.of(sampleAttempt));

        List<QuizAttemptResponse> result = quizAttemptService.findByStudentId("student1");

        assertThat(result).hasSize(1);
    }

    @Test
    void findByQuizId_returnsMappedList() {
        when(quizAttemptRepository.findByQuizId("quiz1")).thenReturn(List.of(sampleAttempt));

        List<QuizAttemptResponse> result = quizAttemptService.findByQuizId("quiz1");

        assertThat(result).hasSize(1);
    }

    @Test
    void findByStudentAndQuiz_returnsMappedList() {
        when(quizAttemptRepository.findByStudentIdAndQuizId("student1", "quiz1"))
                .thenReturn(List.of(sampleAttempt));

        List<QuizAttemptResponse> result = quizAttemptService.findByStudentAndQuiz("student1", "quiz1");

        assertThat(result).hasSize(1);
    }

    @Test
    void findLatestAttempt_found_returnsResponse() {
        when(quizAttemptRepository.findFirstByStudentIdAndQuizIdOrderByAttemptedAtDesc("student1", "quiz1"))
                .thenReturn(sampleAttempt);

        QuizAttemptResponse result = quizAttemptService.findLatestAttempt("student1", "quiz1");

        assertThat(result.getId()).isEqualTo("attempt1");
    }

    @Test
    void findLatestAttempt_notFound_throws() {
        when(quizAttemptRepository.findFirstByStudentIdAndQuizIdOrderByAttemptedAtDesc("s", "q"))
                .thenReturn(null);

        assertThatThrownBy(() -> quizAttemptService.findLatestAttempt("s", "q"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findPassedByQuiz_returnsMappedList() {
        when(quizAttemptRepository.findByQuizIdAndPassed("quiz1", true)).thenReturn(List.of(sampleAttempt));

        List<QuizAttemptResponse> result = quizAttemptService.findPassedByQuiz("quiz1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPassed()).isTrue();
    }

    // ── computeAvgScore ───────────────────────────────────────────────────────

    @Test
    void computeAvgScore_emptyList_returnsZero() {
        when(quizAttemptRepository.findByQuizId("quiz1")).thenReturn(List.of());

        assertThat(quizAttemptService.computeAvgScore("quiz1")).isEqualTo(0.0);
    }

    @Test
    void computeAvgScore_multipleAttempts_returnsAvg() {
        QuizAttempt a1 = QuizAttempt.builder().score(70).build();
        QuizAttempt a2 = QuizAttempt.builder().score(90).build();
        when(quizAttemptRepository.findByQuizId("quiz1")).thenReturn(List.of(a1, a2));

        assertThat(quizAttemptService.computeAvgScore("quiz1")).isEqualTo(80.0);
    }

    // ── hasStudentPassedQuiz / countPassedByQuiz ──────────────────────────────

    @Test
    void hasStudentPassedQuiz_delegatesToRepository() {
        when(quizAttemptRepository.existsByStudentIdAndQuizIdAndPassed("s", "q", true)).thenReturn(true);

        assertThat(quizAttemptService.hasStudentPassedQuiz("s", "q")).isTrue();
    }

    @Test
    void countPassedByQuiz_delegatesToRepository() {
        when(quizAttemptRepository.countByQuizIdAndPassed("quiz1", true)).thenReturn(5L);

        assertThat(quizAttemptService.countPassedByQuiz("quiz1")).isEqualTo(5L);
    }

    @Test
    void deleteById_callsRepository() {
        quizAttemptService.deleteById("attempt1");

        verify(quizAttemptRepository).deleteById("attempt1");
    }

    // ── recalculate progress: all lectures + quiz completed → 100% ────────────

    @Test
    void create_allCompleted_marksEnrollmentAsCompleted() {
        QuizAttemptRequest req = new QuizAttemptRequest();
        req.setStudentId("student1");
        req.setCourseId("course1");
        req.setPassed(true);

        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(sampleAttempt);
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("student1", "course1"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(moduleQuizRepository.findAllByCourseId("course1")).thenReturn(List.of());
        when(quizAttemptRepository.findByStudentIdAndQuizId(eq("student1"), eq("quiz1")))
                .thenReturn(List.of(sampleAttempt));

        quizAttemptService.create(req);

        verify(mongoTemplate).updateFirst(any(), argThat(update ->
                update.toString().contains("completed") || update.toString().contains("100")
        ), eq(Enrollment.class));
    }
}
