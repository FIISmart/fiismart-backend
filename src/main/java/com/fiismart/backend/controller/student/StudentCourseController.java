package com.fiismart.backend.controller.student;

import com.fiismart.backend.dto.student.StudentCourseHeaderDTO;
import com.fiismart.backend.service.student.StudentCourseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students/{studentId}/courses")
@CrossOrigin(origins = "*")
public class StudentCourseController {

    private final StudentCourseService studentCourseService;

    public StudentCourseController(StudentCourseService studentCourseService) {
        this.studentCourseService = studentCourseService;
    }

    @GetMapping("/{courseId}")
    public StudentCourseHeaderDTO getCourseHeader(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        return studentCourseService.getHeader(studentId, courseId);
    }
}