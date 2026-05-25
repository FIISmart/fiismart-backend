package ro.fiismart.dashboard.teacher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.teacher.dto.TeacherCoursesDTO;
import ro.fiismart.dashboard.teacher.service.TeacherCoursesService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSOR')")
public class TeacherCoursesController {

    private final TeacherCoursesService teacherCoursesService;

    @GetMapping("/me/courses")
    public List<TeacherCoursesDTO> getCourses(
            @AuthenticationPrincipal String teacherId,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return teacherCoursesService.getCourses(teacherId, status, limit, offset);
    }
}