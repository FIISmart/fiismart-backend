package ro.fiismart.common.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelBuilderTest {

    @Test
    void user_builder() {
        Date now = new Date();
        User u = User.builder()
                .id("u1").email("a@b.com").displayName("Ion Pop")
                .role("student").cognitoSub("sub1").cognitoUsername("usr")
                .banned(false).needsRoleSelection(false)
                .createdAt(now).lastLoginAt(now)
                .ownedCourses(List.of("c1")).enrolledCourseIds(List.of("c2"))
                .build();
        assertThat(u.getId()).isEqualTo("u1");
        assertThat(u.getEmail()).isEqualTo("a@b.com");
        assertThat(u.getRole()).isEqualTo("student");
        assertThat(u.isBanned()).isFalse();
        u.setBanned(true);
        u.setBanReason("Spam");
        u.setBannedAt(now);
        u.setBannedBy("admin");
        assertThat(u.isBanned()).isTrue();
        assertThat(u.getBanReason()).isEqualTo("Spam");
    }

    @Test
    void course_builder() {
        Course c = Course.builder()
                .id("c1").title("Matematică").description("Desc")
                .teacherId("t1").status("published").hidden(false)
                .tags(List.of("math")).thumbnailUrl("thumb.jpg").language("ro")
                .enrollmentCount(10).avgRating(4.5).quizId("q1")
                .modules(new ArrayList<>())
                .build();
        assertThat(c.getId()).isEqualTo("c1");
        assertThat(c.getTitle()).isEqualTo("Matematică");
        assertThat(c.isHidden()).isFalse();
        c.setHidden(true);
        assertThat(c.isHidden()).isTrue();
    }

    @Test
    void courseModule_builder() {
        Lecture lec = Lecture.builder()
                .id("l1").moduleId("m1").title("Lecție").type("video")
                .videoUrl("http://v").order(0).durationSecs(120)
                .build();
        CourseModule m = CourseModule.builder()
                .id("m1").title("Modul").description("Desc").order(0)
                .quizId("q1").lectures(new ArrayList<>(List.of(lec)))
                .build();
        assertThat(m.getId()).isEqualTo("m1");
        assertThat(m.getLectures()).hasSize(1);
        lec.setPdfUrl("doc.pdf");
        lec.setContent("# Content");
        lec.setImageUrls(List.of("img1.png"));
        assertThat(lec.getPdfUrl()).isEqualTo("doc.pdf");
    }

    @Test
    void enrollment_builder() {
        Date now = new Date();
        LectureProgressEntry prog = LectureProgressEntry.builder()
                .lectureId("l1").watchedPercent(50).positionSecs(100)
                .completed(false).updatedAt(now).build();
        Enrollment e = Enrollment.builder()
                .id("e1").studentId("s1").courseId("c1")
                .status("enrolled").overallProgress(30)
                .enrolledAt(now).lastAccessedAt(now)
                .lectureProgress(new ArrayList<>(List.of(prog)))
                .build();
        assertThat(e.getId()).isEqualTo("e1");
        assertThat(e.getLectureProgress()).hasSize(1);
        e.setCompletedAt(now);
        assertThat(e.getCompletedAt()).isEqualTo(now);
        prog.setCompleted(true);
        assertThat(prog.isCompleted()).isTrue();
    }

    @Test
    void quiz_builder() {
        QuizQuestion q = QuizQuestion.builder()
                .id("q1").text("Ce?").type("single").points(1)
                .options(List.of("A", "B")).correctIdx(0)
                .correctText("A").explanation("Pt că...").build();
        Quiz quiz = Quiz.builder()
                .id("qz1").courseId("c1").title("Quiz").passingScore(70)
                .timeLimit(30).shuffleQuestions(true)
                .questions(new ArrayList<>(List.of(q)))
                .build();
        assertThat(quiz.getId()).isEqualTo("qz1");
        assertThat(quiz.isShuffleQuestions()).isTrue();
        assertThat(q.getText()).isEqualTo("Ce?");
    }

    @Test
    void quizAttempt_builder() {
        Answer a = new Answer();
        a.setQuestionId("q1");
        a.setSelectedIdx(0);
        a.setCorrect(true);
        QuizAttempt attempt = QuizAttempt.builder()
                .id("a1").quizId("q1").courseId("c1").studentId("s1")
                .score(80).passed(true).timeTakenSecs(120)
                .answers(List.of(a))
                .build();
        assertThat(attempt.getId()).isEqualTo("a1");
        assertThat(attempt.isPassed()).isTrue();
        assertThat(a.isCorrect()).isTrue();
        assertThat(a.getQuestionId()).isEqualTo("q1");
    }

    @Test
    void moduleQuiz_builder() {
        ModuleQuizQuestion mq = ModuleQuizQuestion.builder()
                .id("mq1").text("Q?").type("single").points(2)
                .options(List.of("X", "Y")).correctIdx(1)
                .imageUrl("img.png").correctText("Y").explanation("Exp")
                .build();
        ModuleQuiz quiz = ModuleQuiz.builder()
                .id("mq1").courseId("c1").moduleId("m1").lectureId("l1")
                .quizScope("lecture").title("Quiz L").passingScore(70)
                .timeLimit(20).shuffleQuestions(false)
                .questions(new ArrayList<>(List.of(mq)))
                .build();
        assertThat(quiz.getId()).isEqualTo("mq1");
        assertThat(quiz.getQuizScope()).isEqualTo("lecture");
        assertThat(mq.getImageUrl()).isEqualTo("img.png");
    }

    @Test
    void notification_builder() {
        Notification n = Notification.builder()
                .id("n1").recipientId("u1").type("enrollment")
                .title("Înscris").message("Înscris cu succes!")
                .read(false).createdAt(new Date())
                .courseId("c1").lectureId("l1")
                .build();
        assertThat(n.getId()).isEqualTo("n1");
        assertThat(n.getRecipientId()).isEqualTo("u1");
        assertThat(n.isRead()).isFalse();
        n.setRead(true);
        assertThat(n.isRead()).isTrue();
    }

    @Test
    void review_builder() {
        Review r = Review.builder()
                .id("r1").studentId("s1").courseId("c1")
                .stars(5).body("Excelent!").deleted(false)
                .createdAt(new Date()).build();
        assertThat(r.getId()).isEqualTo("r1");
        assertThat(r.getStars()).isEqualTo(5);
        r.setDeleted(true);
        r.setDeletedBy("admin");
        assertThat(r.isDeleted()).isTrue();
    }

    @Test
    void comment_builder() {
        Comment c = Comment.builder()
                .id("c1").lectureId("l1").courseId("co1")
                .authorId("a1").body("Întrebare?")
                .deleted(false).likeCount(3)
                .likedBy(new ArrayList<>())
                .parentCommentId(null)
                .build();
        assertThat(c.getId()).isEqualTo("c1");
        assertThat(c.getLikeCount()).isEqualTo(3);
        c.setLikeCount(5);
        assertThat(c.getLikeCount()).isEqualTo(5);
    }

    @Test
    void session_builder() {
        Date now = new Date();
        Session s = Session.builder()
                .token("tok")
                .createdAt(now)
                .expiresAt(now)
                .build();
        assertThat(s.getToken()).isEqualTo("tok");
        assertThat(s.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void moderationFlag_builder() {
        Date now = new Date();
        ModerationFlag f = ModerationFlag.builder()
                .flaggedBy("u1").reason("Spam").flaggedAt(now)
                .build();
        assertThat(f.getFlaggedBy()).isEqualTo("u1");
        assertThat(f.getReason()).isEqualTo("Spam");
    }
}
