package ro.fiismart.dashboard.teacher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.common.model.QuizAttempt;
import ro.fiismart.common.repository.*;
import ro.fiismart.dashboard.teacher.dto.TeacherStatsDTO;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherStatsServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private QuizRepository quizRepository;

    @InjectMocks
    private TeacherStatsService teacherStatsService;

    @Test
    void getStats_noCourses_returnsZeroes() {
        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of());

        TeacherStatsDTO dto = teacherStatsService.getStats("teacher1");

        assertThat(dto.getStudentsEnrolled()).isEqualTo(0);
        assertThat(dto.getActiveCourses()).isEqualTo(0);
        assertThat(dto.getCompletionRatePct()).isEqualTo(0.0);
    }

    @Test
    void getStats_withPublishedAndDraftCourses_countsOnlyPublished() {
        Course published = Course.builder().id("c1").status("published").hidden(false).build();
        Course draft = Course.builder().id("c2").status("draft").hidden(false).build();
        Course hidden = Course.builder().id("c3").status("published").hidden(true).build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(published, draft, hidden));
        when(enrollmentRepository.findByCourseId(any())).thenReturn(List.of());
        when(quizRepository.findByCourseId(any())).thenReturn(Optional.empty());

        TeacherStatsDTO dto = teacherStatsService.getStats("teacher1");

        assertThat(dto.getActiveCourses()).isEqualTo(1);
    }

    @Test
    void getStats_withEnrollments_calculatesCompletionRate() {
        Course course = Course.builder().id("c1").status("published").hidden(false).build();
        Enrollment completed = Enrollment.builder().status("completed").completedAt(new Date()).build();
        Enrollment active = Enrollment.builder().status("enrolled").build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(course));
        when(enrollmentRepository.findByCourseId("c1")).thenReturn(List.of(completed, active));
        when(quizRepository.findByCourseId("c1")).thenReturn(Optional.empty());

        TeacherStatsDTO dto = teacherStatsService.getStats("teacher1");

        assertThat(dto.getStudentsEnrolled()).isEqualTo(2);
        assertThat(dto.getCompletionRatePct()).isEqualTo(50.0);
    }

    @Test
    void getStats_withQuiz_countsAttempts() {
        Course course = Course.builder().id("c1").status("published").hidden(false).build();
        Quiz quiz = Quiz.builder().id("quiz1").courseId("c1").build();
        QuizAttempt attempt = QuizAttempt.builder().score(80).passed(true).build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(course));
        when(enrollmentRepository.findByCourseId("c1")).thenReturn(List.of());
        when(quizRepository.findByCourseId("c1")).thenReturn(Optional.of(quiz));
        when(quizAttemptRepository.findByQuizId("quiz1")).thenReturn(List.of(attempt));

        TeacherStatsDTO dto = teacherStatsService.getStats("teacher1");

        assertThat(dto.getQuizzesCompleted()).isEqualTo(1);
    }

    @Test
    void getStats_completedByCompletedAt_countsToo() {
        Course course = Course.builder().id("c1").status("published").hidden(false).build();
        Enrollment e = Enrollment.builder().status("enrolled").completedAt(new Date()).build();

        when(courseRepository.findByTeacherId("t1")).thenReturn(List.of(course));
        when(enrollmentRepository.findByCourseId("c1")).thenReturn(List.of(e));
        when(quizRepository.findByCourseId("c1")).thenReturn(Optional.empty());

        TeacherStatsDTO dto = teacherStatsService.getStats("t1");

        assertThat(dto.getCompletionRatePct()).isEqualTo(100.0);
    }
}
