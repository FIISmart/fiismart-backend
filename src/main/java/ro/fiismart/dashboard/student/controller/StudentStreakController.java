package ro.fiismart.dashboard.student.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.util.AuthUtils;
import ro.fiismart.dashboard.student.dto.StreakResponse;
import ro.fiismart.dashboard.student.service.StudentStreakService;

@RestController
@RequestMapping("/api/v1/students/{studentId}")
@PreAuthorize("hasRole('STUDENT')")
public class StudentStreakController {

    private final StudentStreakService studentStreakService;

    public StudentStreakController(StudentStreakService studentStreakService) {
        this.studentStreakService = studentStreakService;
    }

    @GetMapping("/streak")
    public StreakResponse getStreak(@PathVariable String studentId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own data");
        }
        return studentStreakService.calculateStreak(studentId);
    }
}
