package ro.fiismart.dashboard.student.controller;

import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.student.service.StudentStatsService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/students/{studentId}")
public class StudentStreakController {

    private final StudentStatsService statsService;

    public StudentStreakController(StudentStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/streak")
    public Map<String, Object> getStreak(@PathVariable String studentId) {
        return statsService.calculateStreak(studentId);
    }
}