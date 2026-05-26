package ro.fiismart.dashboard.student.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                                                   @PathVariable String courseId,
                                                   @AuthenticationPrincipal String authenticatedUserId) {
        assertOwnStudentPath(studentId, authenticatedUserId);
        return studentCourseService.getHeader(studentId, courseId);
    }

    private void assertOwnStudentPath(String studentId, String authenticatedUserId) {
        if (authenticatedUserId == null || !authenticatedUserId.equals(studentId)) {
            throw new AccessDeniedException("Cannot access another student's course data");
        }
    }
}
