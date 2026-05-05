package ro.fiismart.dashboard.student.controller;

import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.student.dto.StudentCourseHeaderDTO;
import ro.fiismart.dashboard.student.service.StudentCourseService;

@RestController
@RequestMapping("/api/v1/students/{studentId}/courses")
public class StudentCourseController {

    private final StudentCourseService studentCourseService;

    public StudentCourseController(StudentCourseService studentCourseService) {
        this.studentCourseService = studentCourseService;
    }

    @GetMapping("/{courseId}")
    public StudentCourseHeaderDTO getCourseHeader(@PathVariable String studentId,
                                                   @PathVariable String courseId) {
        return studentCourseService.getHeader(studentId, courseId);
    }
}
