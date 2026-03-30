package com.fiismart.teacher_dashboard.controller;

import com.fiismart.teacher_dashboard.dto.TeacherOverviewDTO;
import com.fiismart.teacher_dashboard.service.TeacherOverviewService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher-dashboard")
@CrossOrigin(origins = "*")
public class TeacherOverviewController {

    private final TeacherOverviewService teacherOverviewService;

    public TeacherOverviewController(TeacherOverviewService teacherOverviewService) {
        this.teacherOverviewService = teacherOverviewService;
    }

    @GetMapping("/me/overview")
    public TeacherOverviewDTO getOverview(
            @RequestHeader("X-Dev-UserId") String teacherId,
            @RequestParam(defaultValue = "3") int coursesLimit,
            @RequestParam(defaultValue = "5") int quizzesLimit,
            @RequestParam(defaultValue = "3") int commentsLimit) {

        return teacherOverviewService.getOverview(teacherId, coursesLimit, quizzesLimit, commentsLimit);
    }
}
