package com.fiismart.teacher_dashboard.controller;

import com.fiismart.teacher_dashboard.dto.TeacherCoursesDTO;
import com.fiismart.teacher_dashboard.service.TeacherCoursesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher-dashboard")
@CrossOrigin(origins = "*")
public class TeacherCoursesController {

    private final TeacherCoursesService teacherCoursesService;

    public TeacherCoursesController(TeacherCoursesService teacherCoursesService) {
        this.teacherCoursesService = teacherCoursesService;
    }

    @GetMapping("/me/courses")
    public List<TeacherCoursesDTO> getCourses(
            @RequestHeader("X-Dev-UserId") String teacherId,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "3") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        return teacherCoursesService.getCourses(teacherId, status, limit, offset);
    }
}
