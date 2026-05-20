package ro.fiismart.dashboard.teacher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.common.model.QuizAttempt;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.QuizAttemptRepository;
import ro.fiismart.common.repository.QuizRepository;
import ro.fiismart.dashboard.teacher.dto.TeacherQuizPreviewDTO;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherQuizzesServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @InjectMocks
    private TeacherQuizzesService teacherQuizzesService;

    @Test
    void getQuizzes_courseWithQuiz_returnsDTO() {
        Course course = Course.builder().id("c1").title("Matematică").build();
        Quiz quiz = Quiz.builder().id("quiz1").title("Quiz Final").courseId("c1").build();
        QuizAttempt a1 = QuizAttempt.builder().score(80).build();
        QuizAttempt a2 = QuizAttempt.builder().score(60).build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(course));
        when(quizRepository.findByCourseId("c1")).thenReturn(Optional.of(quiz));
        when(quizAttemptRepository.findByQuizId("quiz1")).thenReturn(List.of(a1, a2));

        List<TeacherQuizPreviewDTO> result = teacherQuizzesService.getQuizzes("teacher1", 10, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuizId()).isEqualTo("quiz1");
        assertThat(result.get(0).getAttemptsCount()).isEqualTo(2);
        assertThat(result.get(0).getAvgScorePct()).isEqualTo(70.0);
        assertThat(result.get(0).getCourseTitle()).isEqualTo("Matematică");
    }

    @Test
    void getQuizzes_courseWithoutQuiz_skipped() {
        Course course = Course.builder().id("c1").title("Fizică").build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(course));
        when(quizRepository.findByCourseId("c1")).thenReturn(Optional.empty());

        List<TeacherQuizPreviewDTO> result = teacherQuizzesService.getQuizzes("teacher1", 10, 0);

        assertThat(result).isEmpty();
    }

    @Test
    void getQuizzes_noAttempts_avgScoreIsZero() {
        Course course = Course.builder().id("c1").title("Chimie").build();
        Quiz quiz = Quiz.builder().id("quiz1").title("Quiz").courseId("c1").build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(course));
        when(quizRepository.findByCourseId("c1")).thenReturn(Optional.of(quiz));
        when(quizAttemptRepository.findByQuizId("quiz1")).thenReturn(List.of());

        List<TeacherQuizPreviewDTO> result = teacherQuizzesService.getQuizzes("teacher1", 10, 0);

        assertThat(result.get(0).getAvgScorePct()).isEqualTo(0.0);
    }

    @Test
    void getQuizzes_withPagination_appliesLimitOffset() {
        Course c1 = Course.builder().id("c1").title("A").build();
        Course c2 = Course.builder().id("c2").title("B").build();
        Quiz quiz1 = Quiz.builder().id("q1").courseId("c1").build();
        Quiz quiz2 = Quiz.builder().id("q2").courseId("c2").build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(c1, c2));
        when(quizRepository.findByCourseId("c1")).thenReturn(Optional.of(quiz1));
        when(quizRepository.findByCourseId("c2")).thenReturn(Optional.of(quiz2));
        when(quizAttemptRepository.findByQuizId(any())).thenReturn(List.of());

        List<TeacherQuizPreviewDTO> result = teacherQuizzesService.getQuizzes("teacher1", 1, 0);

        assertThat(result).hasSize(1);
    }
}
