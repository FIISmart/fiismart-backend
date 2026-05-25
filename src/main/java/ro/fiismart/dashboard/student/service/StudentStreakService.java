package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.dashboard.student.dto.StreakResponse;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentStreakService {

    private final EnrollmentRepository enrollmentRepository;

    public StudentStreakService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public StreakResponse calculateStreak(String studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

        Set<String> accessDays = enrollments.stream()
                .filter(e -> e.getLastAccessedAt() != null)
                .map(e -> toDayKey(e.getLastAccessedAt()))
                .collect(Collectors.toSet());

        Calendar today = Calendar.getInstance();
        String todayKey = toDayKey(today);
        boolean hasCompletedToday = accessDays.contains(todayKey);

        int streak = 0;
        Calendar cursor = Calendar.getInstance();

        while (true) {
            String day = toDayKey(cursor);
            if (accessDays.contains(day)) {
                streak++;
                cursor.add(Calendar.DAY_OF_MONTH, -1);
            } else {
                break;
            }
        }

        return new StreakResponse(streak, hasCompletedToday);
    }

    private String toDayKey(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH);
    }

    private String toDayKey(Calendar calendar) {
        return calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DAY_OF_MONTH);
    }
}
