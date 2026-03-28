package com.fiismart.student.dashboard.service;

import com.fiismart.student.dashboard.dto.StatsDTO;
import database.dao.EnrollmentDAO;
import database.dao.QuizAttemptDAO;
import database.model.Enrollment;
import database.model.QuizAttempt;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final QuizAttemptDAO quizAttemptDAO = new QuizAttemptDAO();

    public StatsDTO getStats(String studentId) {
        ObjectId id = new ObjectId(studentId);

        List<Enrollment> allEnrollments = enrollmentDAO.findByStudentId(id);
        List<Enrollment> completedEnrollments = enrollmentDAO.findCompletedByStudent(id);
        List<QuizAttempt> attempts = quizAttemptDAO.findByStudentId(id);

        StatsDTO dto = new StatsDTO();
        dto.setEnrolledCourses(allEnrollments.size());
        dto.setActiveCourses(allEnrollments.size() - completedEnrollments.size());
        dto.setQuizzesCompleted(attempts.size());
        dto.setStreakDays(calculateStreak(allEnrollments));

        return dto;
    }

    private int calculateStreak(List<Enrollment> enrollments) {
        // Colectăm toate datele de lastAccessedAt
        Set<String> accessDays = enrollments.stream()
                .filter(e -> e.getLastAccessedAt() != null)
                .map(e -> {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(e.getLastAccessedAt());
                    // Normalizăm la zi (fără ore/minute/secunde)
                    return cal.get(Calendar.YEAR) + "-" +
                            cal.get(Calendar.MONTH) + "-" +
                            cal.get(Calendar.DAY_OF_MONTH);
                })
                .collect(Collectors.toSet());

        if (accessDays.isEmpty()) return 0;

        // Calculăm streak-ul pornind de azi în urmă
        int streak = 0;
        Calendar today = Calendar.getInstance();

        while (true) {
            String day = today.get(Calendar.YEAR) + "-" +
                    today.get(Calendar.MONTH) + "-" +
                    today.get(Calendar.DAY_OF_MONTH);

            if (accessDays.contains(day)) {
                streak++;
                today.add(Calendar.DAY_OF_MONTH, -1);
            } else {
                break;
            }
        }

        return streak;
    }
}
