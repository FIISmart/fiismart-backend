package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.QuizAttempt;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.QuizAttemptRepository;
import ro.fiismart.dashboard.student.dto.StatsDTO;

import java.util.Calendar;
import java.util.List;
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
        dto.setStreakDays(calculateStreak(allEnrollments));
        return dto;
    }

    private int calculateStreak(List<Enrollment> enrollments) {
        Set<String> accessDays = enrollments.stream()
                .filter(e -> e.getLastAccessedAt() != null)
                .map(e -> {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(e.getLastAccessedAt());
                    return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH);
                })
                .collect(Collectors.toSet());

        if (accessDays.isEmpty()) return 0;

        int streak = 0;
        Calendar today = Calendar.getInstance();

        while (true) {
            String day = today.get(Calendar.YEAR) + "-" + today.get(Calendar.MONTH) + "-" + today.get(Calendar.DAY_OF_MONTH);
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
