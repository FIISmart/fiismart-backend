package ro.fiismart.dashboard.teacher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.teacher.dto.TeacherStatsDTO;
import ro.fiismart.dashboard.teacher.service.TeacherStatsService;

@RestController
@RequestMapping("/api/v1/teacher-dashboard")
@RequiredArgsConstructor
public class TeacherStatsController {

    private final TeacherStatsService teacherStatsService;

    @GetMapping("/me/stats")
    public TeacherStatsDTO getStats(@AuthenticationPrincipal String teacherId) {
        return teacherStatsService.getStats(teacherId);
    }
}
