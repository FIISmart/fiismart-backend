package ro.fiismart.dashboard.teacher.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.ModuleQuiz;
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
    private final ModuleQuizRepository moduleQuizRepository;

    public TeacherStatsService(CourseRepository courseRepository,
                               EnrollmentRepository enrollmentRepository,
                               QuizAttemptRepository quizAttemptRepository,
                               ModuleQuizRepository moduleQuizRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.moduleQuizRepository = moduleQuizRepository;
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
        int totalQuizzes = 0;
        int lectureQuizzes = 0;
        int moduleQuizzes = 0;
        int finalQuizzes = 0;

        for (String courseId : courseIds) {
            List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
            totalEnrollments += enrollments.size();
            completedEnrollments += (int) enrollments.stream()
                    .filter(e -> "completed".equalsIgnoreCase(e.getStatus()) || e.getCompletedAt() != null)
                    .count();

            List<ModuleQuiz> quizzes = moduleQuizRepository.findAllByCourseId(courseId);
            totalQuizzes += quizzes.size();
            for (ModuleQuiz quiz : quizzes) {
                if ("lecture".equalsIgnoreCase(quiz.getQuizScope())) lectureQuizzes++;
                else if ("module".equalsIgnoreCase(quiz.getQuizScope())) moduleQuizzes++;
                else if ("course_final".equalsIgnoreCase(quiz.getQuizScope())) finalQuizzes++;
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
        dto.setTotalQuizzes(totalQuizzes);
        dto.setLectureQuizzes(lectureQuizzes);
        dto.setModuleQuizzes(moduleQuizzes);
        dto.setFinalQuizzes(finalQuizzes);
        dto.setCompletionRatePct(Math.round(completionRatePct * 10.0) / 10.0);
        return dto;
    }
}
