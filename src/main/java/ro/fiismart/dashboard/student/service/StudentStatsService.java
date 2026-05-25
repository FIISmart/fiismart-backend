package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.QuizAttempt;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.QuizAttemptRepository;
import ro.fiismart.dashboard.student.dto.StatsDTO;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentStatsService {

    private final EnrollmentRepository enrollmentRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public StudentStatsService(EnrollmentRepository enrollmentRepository,
                               QuizAttemptRepository quizAttemptRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public StatsDTO getStats(String studentId) {
        List<Enrollment> allEnrollments = enrollmentRepository.findByStudentId(studentId);
        List<Enrollment> completed = enrollmentRepository.findByStudentIdAndStatus(studentId, "completed");
        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentId(studentId);

        StatsDTO dto = new StatsDTO();
        dto.setEnrolledCourses(allEnrollments.size());
        dto.setActiveCourses(allEnrollments.size() - completed.size());
        dto.setQuizzesCompleted(attempts.size());
        dto.setStreakDays(((Number) calculateStreak(studentId).get("currentStreak")).intValue());
        return dto;
    }

    public Map<String, Object> calculateStreak(String studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

        Set<String> accessDays = enrollments.stream()
                .filter(e -> e.getLastAccessedAt() != null)
                .map(e -> {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(e.getLastAccessedAt());
                    return dayKey(cal);
                })
                .collect(Collectors.toSet());

        int streak = 0;
        Calendar cursor = Calendar.getInstance();

        while (accessDays.contains(dayKey(cursor))) {
            streak++;
            cursor.add(Calendar.DAY_OF_MONTH, -1);
        }

        boolean hasCompletedToday = accessDays.contains(dayKey(Calendar.getInstance()));

        return Map.of("currentStreak", streak, "hasCompletedToday", hasCompletedToday);
    }

    private String dayKey(Calendar cal) {
        return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
    }
}
