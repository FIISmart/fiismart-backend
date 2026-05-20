package ro.fiismart.dashboard.dto;

import org.junit.jupiter.api.Test;
import ro.fiismart.dashboard.student.dto.*;
import ro.fiismart.dashboard.teacher.dto.*;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardDTOTest {

    @Test
    void statsDTO_gettersSetters() {
        StatsDTO dto = new StatsDTO();
        dto.setEnrolledCourses(5);
        dto.setActiveCourses(3);
        dto.setQuizzesCompleted(10);
        dto.setStreakDays(7);
        assertThat(dto.getEnrolledCourses()).isEqualTo(5);
        assertThat(dto.getStreakDays()).isEqualTo(7);
    }

    @Test
    void initialsDTO_gettersSetters() {
        InitialsDTO dto = new InitialsDTO();
        dto.setInitials("IP");
        assertThat(dto.getInitials()).isEqualTo("IP");
    }

    @Test
    void userNameDTO_gettersSetters() {
        UserNameDTO dto = new UserNameDTO();
        dto.setDisplayName("Ion");
        assertThat(dto.getDisplayName()).isEqualTo("Ion");
    }

    @Test
    void continueLearningDTO_gettersSetters() {
        ContinueLearningDTO dto = new ContinueLearningDTO();
        dto.setCursId("c1");
        dto.setTitluCurs("Matematică");
        dto.setProgres(60);
        assertThat(dto.getCursId()).isEqualTo("c1");
        assertThat(dto.getProgres()).isEqualTo(60);
    }

    @Test
    void courseSummaryDTO_gettersSetters() {
        CourseSummaryDTO dto = new CourseSummaryDTO();
        dto.setCourseId("c1");
        dto.setTitle("Curs");
        dto.setOverallProgress(50);
        dto.setThumbnailUrl("thumb.jpg");
        dto.setStatus("enrolled");
        assertThat(dto.getCourseId()).isEqualTo("c1");
        assertThat(dto.getOverallProgress()).isEqualTo(50);
    }

    @Test
    void recommendationDTO_gettersSetters() {
        RecommendationDTO dto = new RecommendationDTO();
        dto.setCourseId("c1");
        dto.setTitle("Rec");
        dto.setDescription("Desc");
        dto.setThumbnailUrl("t.jpg");
        dto.setAvgRating(4.5);
        dto.setEnrollmentCount(10);
        assertThat(dto.getCourseId()).isEqualTo("c1");
        assertThat(dto.getAvgRating()).isEqualTo(4.5);
    }

    @Test
    void quizStudentDTO_fields() {
        QuizStudentDTO dto = new QuizStudentDTO();
        dto.titluQuiz = "Quiz";
        dto.numeCurs = "Curs";
        dto.scor = 80;
        dto.status = "Promovat";
        dto.incercari = 2L;
        assertThat(dto.titluQuiz).isEqualTo("Quiz");
        assertThat(dto.scor).isEqualTo(80);
    }

    @Test
    void studentAnswerDTO_fields() {
        StudentAnswerDTO dto = new StudentAnswerDTO();
        dto.intrebare = "Ce este Pi?";
        dto.raspuns = "3.14";
        dto.autorRaspuns = "Prof. Ion";
        assertThat(dto.intrebare).isEqualTo("Ce este Pi?");
    }

    @Test
    void commentCreateRequest_gettersSetters() {
        CommentCreateRequest req = new CommentCreateRequest();
        req.setBody("Întrebare?");
        req.setPositionSecs(100);
        assertThat(req.getBody()).isEqualTo("Întrebare?");
        assertThat(req.getPositionSecs()).isEqualTo(100);
    }

    @Test
    void studentLectureProgressRequest_gettersSetters() {
        StudentLectureProgressRequest req = new StudentLectureProgressRequest();
        req.setWatchedPercent(75);
        req.setPositionSecs(200);
        req.setCompleted(false);
        assertThat(req.getWatchedPercent()).isEqualTo(75);
        assertThat(req.getPositionSecs()).isEqualTo(200);
        assertThat(req.isCompleted()).isFalse();
    }

    @Test
    void teacherStatsDTO_gettersSetters() {
        TeacherStatsDTO dto = new TeacherStatsDTO();
        dto.setStudentsEnrolled(100);
        dto.setActiveCourses(5);
        dto.setQuizzesCompleted(30);
        dto.setCompletionRatePct(75.5);
        assertThat(dto.getStudentsEnrolled()).isEqualTo(100);
        assertThat(dto.getCompletionRatePct()).isEqualTo(75.5);
    }

    @Test
    void teacherCoursesDTO_gettersSetters() {
        TeacherCoursesDTO dto = new TeacherCoursesDTO();
        dto.setCourseId("c1");
        dto.setTitle("Curs");
        dto.setStatus("published");
        dto.setEnrollmentCount(20);
        dto.setAvgRating(4.2);
        dto.setThumbnailUrl("t.jpg");
        dto.setUpdatedAt(new Date());
        assertThat(dto.getCourseId()).isEqualTo("c1");
        assertThat(dto.getAvgRating()).isEqualTo(4.2);
    }

    @Test
    void teacherQuizPreviewDTO_gettersSetters() {
        TeacherQuizPreviewDTO dto = new TeacherQuizPreviewDTO();
        dto.setQuizId("q1");
        dto.setTitle("Quiz");
        dto.setCourseTitle("Curs");
        dto.setAvgScorePct(78.5);
        dto.setAttemptsCount(15);
        assertThat(dto.getQuizId()).isEqualTo("q1");
        assertThat(dto.getAvgScorePct()).isEqualTo(78.5);
    }

    @Test
    void teacherCommentPreviewDTO_gettersSetters() {
        TeacherCommentPreviewDTO dto = new TeacherCommentPreviewDTO();
        dto.setCommentId("cm1");
        dto.setBody("Întrebare?");
        dto.setAuthorDisplayName("Student");
        dto.setCourseTitle("Matematică");
        dto.setAnswered(false);
        assertThat(dto.getCommentId()).isEqualTo("cm1");
        assertThat(dto.isAnswered()).isFalse();
    }

    @Test
    void teacherOverviewDTO_gettersSetters() {
        TeacherOverviewDTO dto = new TeacherOverviewDTO();
        TeacherStatsDTO stats = new TeacherStatsDTO();
        dto.setStats(stats);
        dto.setCoursesPreview(List.of());
        dto.setQuizzesPreview(List.of());
        dto.setCommentsPreview(List.of());
        assertThat(dto.getStats()).isSameAs(stats);
        assertThat(dto.getCoursesPreview()).isEmpty();
    }
}
