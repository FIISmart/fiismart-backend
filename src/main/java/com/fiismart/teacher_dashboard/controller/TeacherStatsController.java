package com.fiismart.teacher_dashboard.controller;

import com.fiismart.teacher_dashboard.dto.TeacherStatsDTO;
import com.fiismart.teacher_dashboard.service.TeacherStatsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher-dashboard")
@CrossOrigin(origins = "*")
public class TeacherStatsController {
    private final TeacherStatsService teacherStatsService;

    public TeacherStatsController(TeacherStatsService teacherStatsService) {
        this.teacherStatsService = teacherStatsService;
    }

    @GetMapping("/me/stats")
    public TeacherStatsDTO getStats(@RequestHeader("X-Dev-UserId") String teacherId) {
        return teacherStatsService.getStats(teacherId);
    }
}
