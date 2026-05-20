package ro.fiismart.dashboard.student.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.QuizAttempt;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.QuizAttemptRepository;
import ro.fiismart.dashboard.student.dto.StatsDTO;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentStatsServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @InjectMocks
    private StudentStatsService studentStatsService;

    @Test
    void getStats_noData_returnsZeroes() {
        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of());
        when(enrollmentRepository.findByStudentIdAndStatus("s1", "completed")).thenReturn(List.of());
        when(quizAttemptRepository.findByStudentId("s1")).thenReturn(List.of());

        StatsDTO result = studentStatsService.getStats("s1");

        assertThat(result.getEnrolledCourses()).isEqualTo(0);
        assertThat(result.getActiveCourses()).isEqualTo(0);
        assertThat(result.getQuizzesCompleted()).isEqualTo(0);
        assertThat(result.getStreakDays()).isEqualTo(0);
    }

    @Test
    void getStats_withEnrollments_calculatesCorrectly() {
        Enrollment e1 = Enrollment.builder().status("enrolled").build();
        Enrollment e2 = Enrollment.builder().status("enrolled").build();
        Enrollment e3 = Enrollment.builder().status("completed").build();
        QuizAttempt a1 = QuizAttempt.builder().build();
        QuizAttempt a2 = QuizAttempt.builder().build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1, e2, e3));
        when(enrollmentRepository.findByStudentIdAndStatus("s1", "completed")).thenReturn(List.of(e3));
        when(quizAttemptRepository.findByStudentId("s1")).thenReturn(List.of(a1, a2));

        StatsDTO result = studentStatsService.getStats("s1");

        assertThat(result.getEnrolledCourses()).isEqualTo(3);
        assertThat(result.getActiveCourses()).isEqualTo(2);
        assertThat(result.getQuizzesCompleted()).isEqualTo(2);
    }

    @Test
    void getStats_accessedToday_streakIsAtLeastOne() {
        Calendar cal = Calendar.getInstance();
        Enrollment e1 = Enrollment.builder().lastAccessedAt(cal.getTime()).build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1));
        when(enrollmentRepository.findByStudentIdAndStatus("s1", "completed")).thenReturn(List.of());
        when(quizAttemptRepository.findByStudentId("s1")).thenReturn(List.of());

        StatsDTO result = studentStatsService.getStats("s1");

        assertThat(result.getStreakDays()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getStats_noLastAccess_streakIsZero() {
        Enrollment e1 = Enrollment.builder().lastAccessedAt(null).build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1));
        when(enrollmentRepository.findByStudentIdAndStatus("s1", "completed")).thenReturn(List.of());
        when(quizAttemptRepository.findByStudentId("s1")).thenReturn(List.of());

        StatsDTO result = studentStatsService.getStats("s1");

        assertThat(result.getStreakDays()).isEqualTo(0);
    }

    @Test
    void getStats_oldAccess_streakIsZero() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -5);
        Enrollment e1 = Enrollment.builder().lastAccessedAt(cal.getTime()).build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1));
        when(enrollmentRepository.findByStudentIdAndStatus("s1", "completed")).thenReturn(List.of());
        when(quizAttemptRepository.findByStudentId("s1")).thenReturn(List.of());

        StatsDTO result = studentStatsService.getStats("s1");

        assertThat(result.getStreakDays()).isEqualTo(0);
    }
}
