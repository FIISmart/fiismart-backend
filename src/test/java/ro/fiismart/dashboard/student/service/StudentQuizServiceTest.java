package ro.fiismart.dashboard.student.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.*;
import ro.fiismart.common.repository.*;
import ro.fiismart.dashboard.student.dto.StudentPlayableQuizDTO;
import ro.fiismart.dashboard.student.dto.StudentQuizStatusDTO;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentQuizServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private ModuleQuizRepository moduleQuizRepository;
    @Mock private QuizAttemptRepository quizAttemptRepository;

    @InjectMocks
    private StudentQuizService studentQuizService;

    // ── getQuizStatus ──────────────────────────────────────────────────────────

    @Test
    void getQuizStatus_courseNotFound_returnsEmptyDTO() {
        when(courseRepository.findById("c1")).thenReturn(Optional.empty());

        StudentQuizStatusDTO result = studentQuizService.getQuizStatus("s1", "c1");

        assertThat(result.isHasQuiz()).isFalse();
    }

    @Test
    void getQuizStatus_hasFinalModuleQuiz_returnsStatusFromIt() {
        Course course = Course.builder().id("c1").build();
        ModuleQuiz mq = ModuleQuiz.builder()
                .id("mq1").title("Final Quiz").quizScope("course_final").build();

        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));
        when(moduleQuizRepository.findByCourseIdAndQuizScope("c1", "course_final"))
                .thenReturn(Optional.of(mq));
        when(quizAttemptRepository.findByStudentIdAndQuizId("s1", "mq1")).thenReturn(List.of());

        StudentQuizStatusDTO result = studentQuizService.getQuizStatus("s1", "c1");

        assertThat(result.isHasQuiz()).isTrue();
        assertThat(result.getQuizId()).isEqualTo("mq1");
        assertThat(result.getStatus()).isEqualTo("disponibil");
    }

    @Test
    void getQuizStatus_noFinalModuleQuiz_noQuizId_returnsEmpty() {
        Course course = Course.builder().id("c1").quizId(null).build();

        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));
        when(moduleQuizRepository.findByCourseIdAndQuizScope("c1", "course_final"))
                .thenReturn(Optional.empty());

        StudentQuizStatusDTO result = studentQuizService.getQuizStatus("s1", "c1");

        assertThat(result.isHasQuiz()).isFalse();
    }

    @Test
    void getQuizStatus_noFinalModuleQuiz_legacyQuizNotFound_returnsEmpty() {
        Course course = Course.builder().id("c1").quizId("q1").build();

        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));
        when(moduleQuizRepository.findByCourseIdAndQuizScope("c1", "course_final"))
                .thenReturn(Optional.empty());
        when(quizRepository.findById("q1")).thenReturn(Optional.empty());

        StudentQuizStatusDTO result = studentQuizService.getQuizStatus("s1", "c1");

        assertThat(result.isHasQuiz()).isFalse();
    }

    @Test
    void getQuizStatus_legacyQuiz_returnsStatus() {
        Course course = Course.builder().id("c1").quizId("q1").build();
        Quiz quiz = Quiz.builder().id("q1").title("Legacy Quiz").build();

        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));
        when(moduleQuizRepository.findByCourseIdAndQuizScope("c1", "course_final"))
                .thenReturn(Optional.empty());
        when(quizRepository.findById("q1")).thenReturn(Optional.of(quiz));
        when(quizAttemptRepository.findByStudentIdAndQuizId("s1", "q1")).thenReturn(List.of());

        StudentQuizStatusDTO result = studentQuizService.getQuizStatus("s1", "c1");

        assertThat(result.isHasQuiz()).isTrue();
        assertThat(result.getTitle()).isEqualTo("Legacy Quiz");
    }

    // ── getPlayableQuiz ────────────────────────────────────────────────────────

    @Test
    void getPlayableQuiz_foundAsModuleQuiz_returnsDTO() {
        ModuleQuiz mq = ModuleQuiz.builder()
                .id("mq1").title("Quiz M").courseId("c1").quizScope("module").build();

        when(moduleQuizRepository.findById("mq1")).thenReturn(Optional.of(mq));

        StudentPlayableQuizDTO result = studentQuizService.getPlayableQuiz("mq1");

        assertThat(result.getId()).isEqualTo("mq1");
        assertThat(result.getTitle()).isEqualTo("Quiz M");
    }

    @Test
    void getPlayableQuiz_notModuleQuiz_foundAsLegacy_returnsDTO() {
        Quiz quiz = Quiz.builder().id("q1").title("Legacy Q").courseId("c1").build();

        when(moduleQuizRepository.findById("q1")).thenReturn(Optional.empty());
        when(quizRepository.findById("q1")).thenReturn(Optional.of(quiz));

        StudentPlayableQuizDTO result = studentQuizService.getPlayableQuiz("q1");

        assertThat(result.getId()).isEqualTo("q1");
        assertThat(result.getQuizScope()).isEqualTo("course_final");
    }

    @Test
    void getPlayableQuiz_notFound_throws() {
        when(moduleQuizRepository.findById("nope")).thenReturn(Optional.empty());
        when(quizRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentQuizService.getPlayableQuiz("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── buildStatus ────────────────────────────────────────────────────────────

    @Test
    void buildStatus_nullQuizId_returnsNoQuiz() {
        StudentQuizStatusDTO result = studentQuizService.buildStatus(null, "T", "scope", null, null, "s1");

        assertThat(result.isHasQuiz()).isFalse();
    }

    @Test
    void buildStatus_blankQuizId_returnsNoQuiz() {
        StudentQuizStatusDTO result = studentQuizService.buildStatus("   ", "T", "scope", null, null, "s1");

        assertThat(result.isHasQuiz()).isFalse();
    }

    @Test
    void buildStatus_noAttempts_returnsDisponibil() {
        when(quizAttemptRepository.findByStudentIdAndQuizId("s1", "q1")).thenReturn(List.of());

        StudentQuizStatusDTO result = studentQuizService.buildStatus("q1", "Title", "module", "m1", "l1", "s1");

        assertThat(result.isHasQuiz()).isTrue();
        assertThat(result.getStatus()).isEqualTo("disponibil");
        assertThat(result.getAttemptCount()).isEqualTo(0);
        assertThat(result.getPassed()).isFalse();
    }

    @Test
    void buildStatus_hasPassed_returnsPromovat() {
        QuizAttempt attempt = QuizAttempt.builder()
                .studentId("s1").quizId("q1").score(80).passed(true).build();
        QuizAttempt latest = QuizAttempt.builder()
                .studentId("s1").quizId("q1").score(80).passed(true).build();

        when(quizAttemptRepository.findByStudentIdAndQuizId("s1", "q1")).thenReturn(List.of(attempt));
        when(quizAttemptRepository.findTopByStudentIdAndQuizIdOrderByAttemptedAtDesc("s1", "q1"))
                .thenReturn(Optional.of(latest));

        StudentQuizStatusDTO result = studentQuizService.buildStatus("q1", "T", "scope", null, null, "s1");

        assertThat(result.getStatus()).isEqualTo("promovat");
        assertThat(result.getPassed()).isTrue();
        assertThat(result.getLatestScore()).isEqualTo(80);
    }

    @Test
    void buildStatus_hasFailed_returnsPicat() {
        QuizAttempt attempt = QuizAttempt.builder()
                .studentId("s1").quizId("q1").score(30).passed(false).build();
        QuizAttempt latest = QuizAttempt.builder()
                .studentId("s1").quizId("q1").score(30).passed(false).build();

        when(quizAttemptRepository.findByStudentIdAndQuizId("s1", "q1")).thenReturn(List.of(attempt));
        when(quizAttemptRepository.findTopByStudentIdAndQuizIdOrderByAttemptedAtDesc("s1", "q1"))
                .thenReturn(Optional.of(latest));

        StudentQuizStatusDTO result = studentQuizService.buildStatus("q1", "T", "scope", null, null, "s1");

        assertThat(result.getStatus()).isEqualTo("picat");
        assertThat(result.getPassed()).isFalse();
    }

    @Test
    void buildStatus_latestAttemptNotFound_latestScoreNull() {
        QuizAttempt attempt = QuizAttempt.builder()
                .studentId("s1").quizId("q1").score(50).passed(false).build();

        when(quizAttemptRepository.findByStudentIdAndQuizId("s1", "q1")).thenReturn(List.of(attempt));
        when(quizAttemptRepository.findTopByStudentIdAndQuizIdOrderByAttemptedAtDesc("s1", "q1"))
                .thenReturn(Optional.empty());

        StudentQuizStatusDTO result = studentQuizService.buildStatus("q1", "T", "scope", null, null, "s1");

        assertThat(result.getLatestScore()).isNull();
    }
}
