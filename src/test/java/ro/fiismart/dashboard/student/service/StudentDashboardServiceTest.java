package ro.fiismart.dashboard.student.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.model.*;
import ro.fiismart.common.repository.*;
import ro.fiismart.dashboard.student.dto.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentDashboardServiceTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudentDashboardService studentDashboardService;

    // ── getQuizzesForStudent ──────────────────────────────────────────────────

    @Test
    void getQuizzesForStudent_noAttempts_returnsEmpty() {
        when(quizAttemptRepository.findByStudentId("s1")).thenReturn(List.of());

        List<QuizStudentDTO> result = studentDashboardService.getQuizzesForStudent("s1");

        assertThat(result).isEmpty();
    }

    @Test
    void getQuizzesForStudent_withAttempts_returnsMappedDTOs() {
        QuizAttempt attempt = QuizAttempt.builder()
                .quizId("quiz1").courseId("c1").score(85).passed(true).build();
        Quiz quiz = Quiz.builder().id("quiz1").title("Quiz Matematică").build();
        Course course = Course.builder().id("c1").title("Matematică").build();

        when(quizAttemptRepository.findByStudentId("s1")).thenReturn(List.of(attempt));
        when(quizRepository.findById("quiz1")).thenReturn(Optional.of(quiz));
        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));
        when(quizAttemptRepository.countByStudentIdAndQuizId("s1", "quiz1")).thenReturn(1L);

        List<QuizStudentDTO> result = studentDashboardService.getQuizzesForStudent("s1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).titluQuiz).isEqualTo("Quiz Matematică");
        assertThat(result.get(0).numeCurs).isEqualTo("Matematică");
        assertThat(result.get(0).scor).isEqualTo(85);
        assertThat(result.get(0).status).isEqualTo("Promovat");
    }

    @Test
    void getQuizzesForStudent_quizAndCourseNotFound_usesDefaults() {
        QuizAttempt attempt = QuizAttempt.builder()
                .quizId("noQuiz").courseId("noCourse").score(50).passed(false).build();

        when(quizAttemptRepository.findByStudentId("s1")).thenReturn(List.of(attempt));
        when(quizRepository.findById("noQuiz")).thenReturn(Optional.empty());
        when(courseRepository.findById("noCourse")).thenReturn(Optional.empty());
        when(quizAttemptRepository.countByStudentIdAndQuizId("s1", "noQuiz")).thenReturn(0L);

        List<QuizStudentDTO> result = studentDashboardService.getQuizzesForStudent("s1");

        assertThat(result.get(0).titluQuiz).isEqualTo("Quiz Necunoscut");
        assertThat(result.get(0).numeCurs).isEqualTo("Curs Necunoscut");
        assertThat(result.get(0).status).isEqualTo("Picat");
    }

    // ── getLastAccessedCourse ─────────────────────────────────────────────────

    @Test
    void getLastAccessedCourse_noEnrollments_returnsNull() {
        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of());

        ContinueLearningDTO result = studentDashboardService.getLastAccessedCourse("s1");

        assertThat(result).isNull();
    }

    @Test
    void getLastAccessedCourse_withLastAccess_returnsMostRecent() {
        Date older = new Date(1000);
        Date newer = new Date(2000);
        Enrollment e1 = Enrollment.builder().courseId("c1").overallProgress(30)
                .lastAccessedAt(older).build();
        Enrollment e2 = Enrollment.builder().courseId("c2").overallProgress(60)
                .lastAccessedAt(newer).build();
        Course c2 = Course.builder().id("c2").title("Fizică").build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1, e2));
        when(courseRepository.findById("c2")).thenReturn(Optional.of(c2));

        ContinueLearningDTO result = studentDashboardService.getLastAccessedCourse("s1");

        assertThat(result).isNotNull();
        assertThat(result.getCursId()).isEqualTo("c2");
        assertThat(result.getProgres()).isEqualTo(60);
    }

    @Test
    void getLastAccessedCourse_noLastAccess_usesFirstEnrollment() {
        Enrollment e1 = Enrollment.builder().courseId("c1").overallProgress(10).build();
        Course c1 = Course.builder().id("c1").title("Chimie").build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1));
        when(courseRepository.findById("c1")).thenReturn(Optional.of(c1));

        ContinueLearningDTO result = studentDashboardService.getLastAccessedCourse("s1");

        assertThat(result).isNotNull();
        assertThat(result.getCursId()).isEqualTo("c1");
    }

    @Test
    void getLastAccessedCourse_courseNotFound_returnsPartialDTO() {
        Enrollment e1 = Enrollment.builder().courseId("noId").overallProgress(50)
                .lastAccessedAt(new Date()).build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1));
        when(courseRepository.findById("noId")).thenReturn(Optional.empty());

        ContinueLearningDTO result = studentDashboardService.getLastAccessedCourse("s1");

        assertThat(result).isNotNull();
        assertThat(result.getCursId()).isNull();
        assertThat(result.getProgres()).isEqualTo(50);
    }

    // ── getAnswersForStudent ──────────────────────────────────────────────────

    @Test
    void getAnswersForStudent_noComments_returnsEmpty() {
        when(commentRepository.findByAuthorId("s1")).thenReturn(List.of());

        List<StudentAnswerDTO> result = studentDashboardService.getAnswersForStudent("s1");

        assertThat(result).isEmpty();
    }

    @Test
    void getAnswersForStudent_withReplies_returnsMappedDTOs() {
        Comment question = Comment.builder().id("q1").body("Ce este Pi?").build();
        Comment reply = Comment.builder().id("r1").body("3.14").authorId("teacher1").build();
        User teacher = User.builder().id("teacher1").displayName("Prof. Ion").build();

        when(commentRepository.findByAuthorId("s1")).thenReturn(List.of(question));
        when(commentRepository.findRepliesByParentId("q1")).thenReturn(List.of(reply));
        when(userRepository.findById("teacher1")).thenReturn(Optional.of(teacher));

        List<StudentAnswerDTO> result = studentDashboardService.getAnswersForStudent("s1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).intrebare).isEqualTo("Ce este Pi?");
        assertThat(result.get(0).raspuns).isEqualTo("3.14");
        assertThat(result.get(0).autorRaspuns).isEqualTo("Prof. Ion");
    }

    @Test
    void getAnswersForStudent_authorNotFound_usesDefault() {
        Comment question = Comment.builder().id("q1").body("Intrebare?").build();
        Comment reply = Comment.builder().id("r1").body("Raspuns").authorId("noUser").build();

        when(commentRepository.findByAuthorId("s1")).thenReturn(List.of(question));
        when(commentRepository.findRepliesByParentId("q1")).thenReturn(List.of(reply));
        when(userRepository.findById("noUser")).thenReturn(Optional.empty());

        List<StudentAnswerDTO> result = studentDashboardService.getAnswersForStudent("s1");

        assertThat(result.get(0).autorRaspuns).isEqualTo("Utilizator necunoscut");
    }

    // ── getStudentName ────────────────────────────────────────────────────────

    @Test
    void getStudentName_found_returnsFirstName() {
        User user = User.builder().id("s1").displayName("Ion Popescu").build();
        when(userRepository.findById("s1")).thenReturn(Optional.of(user));

        UserNameDTO result = studentDashboardService.getStudentName("s1");

        assertThat(result.getDisplayName()).isEqualTo("Ion");
    }

    @Test
    void getStudentName_notFound_returnsDefault() {
        when(userRepository.findById("noId")).thenReturn(Optional.empty());

        UserNameDTO result = studentDashboardService.getStudentName("noId");

        assertThat(result.getDisplayName()).isEqualTo("User Necunoscut");
    }

    @Test
    void getStudentName_nullDisplayName_returnsDefault() {
        User user = User.builder().id("s1").displayName(null).build();
        when(userRepository.findById("s1")).thenReturn(Optional.of(user));

        UserNameDTO result = studentDashboardService.getStudentName("s1");

        assertThat(result.getDisplayName()).isEqualTo("User Necunoscut");
    }
}
