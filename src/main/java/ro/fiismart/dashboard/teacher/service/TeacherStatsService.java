package ro.fiismart.dashboard.teacher.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.common.model.QuizAttempt;
import ro.fiismart.common.repository.*;
import ro.fiismart.dashboard.teacher.dto.TeacherStatsDTO;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherStatsService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;

    public TeacherStatsService(CourseRepository courseRepository,
                               EnrollmentRepository enrollmentRepository,
                               QuizAttemptRepository quizAttemptRepository,
                               QuizRepository quizRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizRepository = quizRepository;
    }

    public TeacherStatsDTO getStats(String teacherId) {
        List<Course> courses = courseRepository.findByTeacherId(teacherId);

        int activeCourses = (int) courses.stream()
                .filter(c -> "published".equalsIgnoreCase(c.getStatus()) && !c.isHidden())
                .count();

        List<String> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        int totalEnrollments = 0;
        int completedEnrollments = 0;
        int quizzesCompleted = 0;

        for (String courseId : courseIds) {
            List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
            totalEnrollments += enrollments.size();
            completedEnrollments += (int) enrollments.stream()
                    .filter(e -> "completed".equalsIgnoreCase(e.getStatus()) || e.getCompletedAt() != null)
                    .count();

            Quiz quiz = quizRepository.findByCourseId(courseId).orElse(null);
            if (quiz != null) {
                quizzesCompleted += quizAttemptRepository.findByQuizId(quiz.getId()).size();
            }
        }

        double completionRatePct = totalEnrollments > 0
                ? (double) completedEnrollments / totalEnrollments * 100
                : 0;

        TeacherStatsDTO dto = new TeacherStatsDTO();
        dto.setStudentsEnrolled(totalEnrollments);
        dto.setActiveCourses(activeCourses);
        dto.setQuizzesCompleted(quizzesCompleted);
        dto.setCompletionRatePct(Math.round(completionRatePct * 10.0) / 10.0);
        return dto;
    }
}
