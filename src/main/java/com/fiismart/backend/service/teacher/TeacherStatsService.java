package com.fiismart.backend.service.teacher;

import com.fiismart.backend.dto.teacher.TeacherStatsDTO;
import database.dao.CourseDAO;
import database.dao.EnrollmentDAO;
import database.dao.QuizAttemptDAO;
import database.dao.QuizDAO;
import database.model.Quiz;
import database.model.Course;
import database.model.Enrollment;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherStatsService {
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final QuizAttemptDAO quizAttemptDAO = new QuizAttemptDAO();
    private final QuizDAO quizDAO = new QuizDAO();

    public TeacherStatsDTO getStats(String teacherIdHex) {
        ObjectId teacherId = new ObjectId(teacherIdHex);

        List<Course> courses = courseDAO.findByTeacherId(teacherId);

        int activeCourses = (int) courses.stream()
                .filter(c -> "published".equalsIgnoreCase(c.getStatus()) && !c.isHidden())
                .count();

        List<ObjectId> courseIds = courses.stream()
                .map(Course::getId)
                .collect(Collectors.toList());

        int totalEnrollments = 0;
        int completedEnrollments = 0;
        int quizzesCompleted = 0;

        for (ObjectId courseId : courseIds) {
            List<Enrollment> enrollments = enrollmentDAO.findByCourseId(courseId);
            totalEnrollments += enrollments.size();
            completedEnrollments += (int) enrollments.stream()
                    .filter(e -> "completed".equalsIgnoreCase(e.getStatus()) || e.getCompletedAt() != null)
                    .count();
            Quiz quiz = quizDAO.findByCourseId(courseId);
            if (quiz != null) {
                quizzesCompleted += quizAttemptDAO.findByQuizId(quiz.getId()).size();
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
