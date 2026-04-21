package com.fiismart.backend.controller.teacher;

import com.fiismart.backend.dto.teacher.TeacherCoursesDTO;
import com.fiismart.backend.service.teacher.TeacherCoursesService;
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
