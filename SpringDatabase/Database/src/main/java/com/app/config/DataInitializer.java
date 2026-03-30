package com.app.config;

import com.app.dto.request.*;
import com.app.dto.response.*;
import com.app.model.Answer;
import com.app.model.LectureProgress;
import com.app.model.ModerationFlag;
import com.app.model.Session;
import com.app.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final CourseService courseService;
    private final QuizService quizService;
    private final EnrollmentService enrollmentService;
    private final ReviewService reviewService;
    private final QuizAttemptService quizAttemptService;
    private final CommentService commentService;

    @Override
    public void run(String... args) {
        System.out.println("=== FIISmart DataInitializer: starting CRUD verification ===");

        verifyUsers();
        verifyCourses();
        verifyQuizzes();
        verifyEnrollments();
        verifyReviews();
        verifyQuizAttempts();
        verifyComments();

        System.out.println("=== DataInitializer: all verifications passed ===");
    }

    private void verifyUsers() {
        System.out.println("[Users] Creating teacher and student...");

        UserResponse teacher = userService.create(UserRequest.builder()
                .displayName("Prof. Ionescu")
                .email("ionescu.teacher@fiismart.ro")
                .role("teacher")
                .password("parola123")
                .build());
        assert teacher.getId() != null : "Teacher ID must not be null";
        System.out.println("[Users] Teacher created: " + teacher.getId());

        UserResponse student = userService.create(UserRequest.builder()
                .displayName("Ana Popescu")
                .email("ana.student@fiismart.ro")
                .role("student")
                .password("parola456")
                .build());
        assert student.getId() != null : "Student ID must not be null";
        System.out.println("[Users] Student created: " + student.getId());

        UserResponse found = userService.findById(teacher.getId());
        assert "teacher".equals(found.getRole()) : "Role mismatch";

        userService.updateDisplayName(teacher.getId(), "Prof. Ionescu Updated");
        userService.banUser(student.getId(), teacher.getId(), "Test ban");
        UserResponse banned = userService.findById(student.getId());
        assert banned.isBanned() : "Student should be banned";

        userService.unbanUser(student.getId());
        assert !userService.findById(student.getId()).isBanned() : "Student should be unbanned";

        userService.addSession(student.getId(), Session.builder()
                .token("test-token-abc")
                .createdAt(new Date())
                .build());
        userService.removeSession(student.getId(), "test-token-abc");

        assert userService.existsByEmail("ionescu.teacher@fiismart.ro") : "Email should exist";
        assert userService.countByRole("teacher") >= 1 : "At least 1 teacher";

        userService.deleteById(teacher.getId());
        userService.deleteById(student.getId());
        System.out.println("[Users] OK");
    }

    private void verifyCourses() {
        System.out.println("[Courses] Creating teacher and course...");

        UserResponse teacher = userService.create(UserRequest.builder()
                .displayName("Teacher Cursuri")
                .email("teacher.courses@fiismart.ro")
                .role("teacher")
                .password("parola123")
                .build());

        CourseResponse course = courseService.create(CourseRequest.builder()
                .title("Java pentru Incepatori")
                .description("Curs complet de Java")
                .teacherId(teacher.getId())
                .status("draft")
                .tags(List.of("java", "programare"))
                .language("ro")
                .build());
        assert course.getId() != null : "Course ID must not be null";
        System.out.println("[Courses] Course created: " + course.getId());

        courseService.updateTitle(course.getId(), "Java Avansat");
        courseService.updateStatus(course.getId(), "published");
        courseService.setHidden(course.getId(), false);

        LectureResponse lecture = courseService.addLecture(course.getId(), LectureRequest.builder()
                .title("Lectia 1: Introducere")
                .videoUrl("https://video.example.com/l1")
                .order(1)
                .durationSecs(600)
                .build());
        assert lecture.getId() != null : "Lecture ID must not be null";

        List<LectureResponse> lectures = courseService.findLecturesByCourseId(course.getId());
        assert !lectures.isEmpty() : "Course should have lectures";

        courseService.removeLecture(course.getId(), lecture.getId());
        courseService.incrementEnrollmentCount(course.getId());
        courseService.decrementEnrollmentCount(course.getId());

        assert !courseService.findPublishedVisible().isEmpty() : "Should find published courses";
        assert courseService.countByTeacher(teacher.getId()) >= 1 : "Teacher should have courses";

        courseService.deleteById(course.getId());
        userService.deleteById(teacher.getId());
        System.out.println("[Courses] OK");
    }

    private void verifyQuizzes() {
        System.out.println("[Quizzes] Creating quiz...");

        UserResponse teacher = userService.create(UserRequest.builder()
                .displayName("Teacher Quiz")
                .email("teacher.quiz@fiismart.ro")
                .role("teacher")
                .password("parola123")
                .build());

        CourseResponse course = courseService.create(CourseRequest.builder()
                .title("Curs Quiz Test")
                .teacherId(teacher.getId())
                .status("published")
                .build());

        QuizResponse quiz = quizService.create(QuizRequest.builder()
                .courseId(course.getId())
                .title("Quiz Final")
                .passingScore(70)
                .timeLimit(30)
                .shuffleQuestions(true)
                .build());
        assert quiz.getId() != null : "Quiz ID must not be null";
        System.out.println("[Quizzes] Quiz created: " + quiz.getId());

        QuizQuestionResponse question = quizService.addQuestion(quiz.getId(), QuizQuestionRequest.builder()
                .text("Ce este Java?")
                .type("multiple_choice")
                .points(10)
                .options(List.of("Un limbaj", "Un cafea", "O planeta", "Un sistem"))
                .correctIdx(0)
                .explanation("Java este un limbaj de programare.")
                .build());
        assert question.getId() != null : "Question ID must not be null";

        assert !quizService.findQuestions(quiz.getId()).isEmpty() : "Quiz should have questions";
        quizService.removeQuestion(quiz.getId(), question.getId());

        quizService.updatePassingScore(quiz.getId(), 80);
        assert quizService.existsByCourseId(course.getId()) : "Quiz should exist for course";

        quizService.deleteById(quiz.getId());
        courseService.deleteById(course.getId());
        userService.deleteById(teacher.getId());
        System.out.println("[Quizzes] OK");
    }

    private void verifyEnrollments() {
        System.out.println("[Enrollments] Creating enrollment...");

        UserResponse teacher = userService.create(UserRequest.builder()
                .displayName("Teacher Enroll")
                .email("teacher.enroll@fiismart.ro")
                .role("teacher")
                .password("parola123")
                .build());

        UserResponse student = userService.create(UserRequest.builder()
                .displayName("Student Enroll")
                .email("student.enroll@fiismart.ro")
                .role("student")
                .password("parola456")
                .build());

        CourseResponse course = courseService.create(CourseRequest.builder()
                .title("Curs Enrollment Test")
                .teacherId(teacher.getId())
                .status("published")
                .build());

        EnrollmentResponse enrollment = enrollmentService.create(EnrollmentRequest.builder()
                .studentId(student.getId())
                .courseId(course.getId())
                .build());
        assert enrollment.getId() != null : "Enrollment ID must not be null";
        System.out.println("[Enrollments] Enrollment created: " + enrollment.getId());

        enrollmentService.addLectureProgress(enrollment.getId(), LectureProgress.builder()
                .lectureId("test-lecture-id")
                .watchedSecs(120)
                .completed(false)
                .lastWatchedAt(new Date())
                .build());

        enrollmentService.updateOverallProgress(enrollment.getId(), 50);
        enrollmentService.updateLastAccessedAt(enrollment.getId(), new Date());

        assert enrollmentService.isEnrolled(student.getId(), course.getId()) : "Should be enrolled";
        assert enrollmentService.countByCourse(course.getId()) >= 1 : "Course should have enrollments";

        enrollmentService.markCompleted(enrollment.getId());
        EnrollmentResponse completed = enrollmentService.findById(enrollment.getId());
        assert "completed".equals(completed.getStatus()) : "Status should be completed";
        assert completed.getOverallProgress() == 100 : "Progress should be 100";

        enrollmentService.deleteById(enrollment.getId());
        courseService.deleteById(course.getId());
        userService.deleteById(teacher.getId());
        userService.deleteById(student.getId());
        System.out.println("[Enrollments] OK");
    }

    private void verifyReviews() {
        System.out.println("[Reviews] Creating review...");

        UserResponse teacher = userService.create(UserRequest.builder()
                .displayName("Teacher Review")
                .email("teacher.review@fiismart.ro")
                .role("teacher")
                .password("parola123")
                .build());

        UserResponse student = userService.create(UserRequest.builder()
                .displayName("Student Review")
                .email("student.review@fiismart.ro")
                .role("student")
                .password("parola456")
                .build());

        CourseResponse course = courseService.create(CourseRequest.builder()
                .title("Curs Review Test")
                .teacherId(teacher.getId())
                .status("published")
                .build());

        ReviewResponse review = reviewService.create(ReviewRequest.builder()
                .studentId(student.getId())
                .courseId(course.getId())
                .stars(5)
                .body("Curs excelent!")
                .build());
        assert review.getId() != null : "Review ID must not be null";
        System.out.println("[Reviews] Review created: " + review.getId());

        double avg = reviewService.computeAvgRating(course.getId());
        assert avg == 5.0 : "Average rating should be 5.0";

        assert reviewService.hasStudentReviewedCourse(student.getId(), course.getId()) : "Student should have reviewed";

        reviewService.updateReview(review.getId(), 4, "Curs bun, dar poate fi imbunatatit.");
        reviewService.softDelete(review.getId(), teacher.getId());

        ReviewResponse deleted = reviewService.findById(review.getId());
        assert deleted.isDeleted() : "Review should be soft deleted";

        reviewService.deleteById(review.getId());
        courseService.deleteById(course.getId());
        userService.deleteById(teacher.getId());
        userService.deleteById(student.getId());
        System.out.println("[Reviews] OK");
    }

    private void verifyQuizAttempts() {
        System.out.println("[QuizAttempts] Creating attempt...");

        UserResponse teacher = userService.create(UserRequest.builder()
                .displayName("Teacher Attempt")
                .email("teacher.attempt@fiismart.ro")
                .role("teacher")
                .password("parola123")
                .build());

        UserResponse student = userService.create(UserRequest.builder()
                .displayName("Student Attempt")
                .email("student.attempt@fiismart.ro")
                .role("student")
                .password("parola456")
                .build());

        CourseResponse course = courseService.create(CourseRequest.builder()
                .title("Curs Attempt Test")
                .teacherId(teacher.getId())
                .status("published")
                .build());

        QuizResponse quiz = quizService.create(QuizRequest.builder()
                .courseId(course.getId())
                .title("Quiz Attempt")
                .passingScore(70)
                .timeLimit(30)
                .build());

        QuizAttemptResponse attempt = quizAttemptService.create(QuizAttemptRequest.builder()
                .quizId(quiz.getId())
                .courseId(course.getId())
                .studentId(student.getId())
                .score(85)
                .passed(true)
                .timeTakenSecs(900)
                .answers(List.of(Answer.builder()
                        .questionId("q1")
                        .selectedIdx(0)
                        .correct(true)
                        .build()))
                .build());
        assert attempt.getId() != null : "Attempt ID must not be null";
        System.out.println("[QuizAttempts] Attempt created: " + attempt.getId());

        assert quizAttemptService.hasStudentPassedQuiz(student.getId(), quiz.getId()) : "Student should have passed";
        assert quizAttemptService.countPassedByQuiz(quiz.getId()) >= 1 : "Should have at least 1 passed";

        double avg = quizAttemptService.computeAvgScore(quiz.getId());
        assert avg == 85.0 : "Average score should be 85";

        QuizAttemptResponse latest = quizAttemptService.findLatestAttempt(student.getId(), quiz.getId());
        assert latest != null : "Latest attempt should not be null";

        quizAttemptService.deleteById(attempt.getId());
        quizService.deleteById(quiz.getId());
        courseService.deleteById(course.getId());
        userService.deleteById(teacher.getId());
        userService.deleteById(student.getId());
        System.out.println("[QuizAttempts] OK");
    }

    private void verifyComments() {
        System.out.println("[Comments] Creating comment...");

        UserResponse teacher = userService.create(UserRequest.builder()
                .displayName("Teacher Comment")
                .email("teacher.comment@fiismart.ro")
                .role("teacher")
                .password("parola123")
                .build());

        UserResponse student = userService.create(UserRequest.builder()
                .displayName("Student Comment")
                .email("student.comment@fiismart.ro")
                .role("student")
                .password("parola456")
                .build());

        CourseResponse course = courseService.create(CourseRequest.builder()
                .title("Curs Comment Test")
                .teacherId(teacher.getId())
                .status("published")
                .build());

        LectureResponse lecture = courseService.addLecture(course.getId(), LectureRequest.builder()
                .title("Lectia 1")
                .order(1)
                .build());

        CommentResponse comment = commentService.create(CommentRequest.builder()
                .lectureId(lecture.getId())
                .courseId(course.getId())
                .authorId(student.getId())
                .body("Intrebare: cum functioneaza mostenirea?")
                .build());
        assert comment.getId() != null : "Comment ID must not be null";
        System.out.println("[Comments] Comment created: " + comment.getId());

        CommentResponse reply = commentService.create(CommentRequest.builder()
                .lectureId(lecture.getId())
                .courseId(course.getId())
                .authorId(teacher.getId())
                .body("Mostenirea se face cu extends.")
                .parentCommentId(comment.getId())
                .build());
        assert reply.getId() != null : "Reply ID must not be null";

        commentService.addLike(comment.getId(), teacher.getId());
        assert commentService.hasUserLiked(comment.getId(), teacher.getId()) : "Teacher should have liked";

        commentService.removeLike(comment.getId(), teacher.getId());
        assert !commentService.hasUserLiked(comment.getId(), teacher.getId()) : "Teacher should have unliked";

        commentService.addModerationFlag(comment.getId(), ModerationFlag.builder()
                .flaggedBy(teacher.getId())
                .reason("Spam")
                .flaggedAt(new Date())
                .build());

        List<CommentResponse> flagged = commentService.findFlagged(1);
        assert !flagged.isEmpty() : "Should find flagged comments";

        commentService.clearModerationFlags(comment.getId());

        commentService.updateBody(comment.getId(), "Intrebare actualizata");
        commentService.softDelete(reply.getId());

        assert commentService.countByLecture(lecture.getId()) >= 1 : "Lecture should have comments";

        commentService.deleteById(comment.getId());
        commentService.deleteById(reply.getId());
        courseService.deleteById(course.getId());
        userService.deleteById(teacher.getId());
        userService.deleteById(student.getId());
        System.out.println("[Comments] OK");
    }
}
