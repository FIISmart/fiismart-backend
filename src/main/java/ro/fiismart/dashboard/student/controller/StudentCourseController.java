package ro.fiismart.dashboard.student.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.util.AuthUtils;
import ro.fiismart.dashboard.student.dto.StudentCourseHeaderDTO;
import ro.fiismart.dashboard.student.service.StudentCourseService;

@RestController
@RequestMapping("/api/v1/students/{studentId}/courses")
@PreAuthorize("hasRole('STUDENT')")
public class StudentCourseController {

    private final StudentCourseService studentCourseService;

    public StudentCourseController(StudentCourseService studentCourseService) {
        this.studentCourseService = studentCourseService;
    }

    @GetMapping("/{courseId}")
    public StudentCourseHeaderDTO getCourseHeader(@PathVariable String studentId,
                                                   @PathVariable String courseId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own data");
        }
        return studentCourseService.getHeader(studentId, courseId);
    }
}