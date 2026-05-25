package ro.fiismart.dashboard.teacher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.teacher.dto.TeacherOverviewDTO;
import ro.fiismart.dashboard.teacher.service.TeacherOverviewService;

@RestController
@RequestMapping("/api/v1/teacher-dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSOR')")
public class TeacherOverviewController {

    private final TeacherOverviewService teacherOverviewService;

    @GetMapping("/me/overview")
    public TeacherOverviewDTO getOverview(
            @AuthenticationPrincipal String teacherId,
            @RequestParam(defaultValue = "3") int coursesLimit,
            @RequestParam(defaultValue = "5") int quizzesLimit,
            @RequestParam(defaultValue = "3") int commentsLimit) {
        return teacherOverviewService.getOverview(teacherId, coursesLimit, quizzesLimit, commentsLimit);
    }
}