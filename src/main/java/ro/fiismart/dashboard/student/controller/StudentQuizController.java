package ro.fiismart.dashboard.student.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.student.dto.StudentQuizStatusDTO;
import ro.fiismart.dashboard.student.service.StudentQuizService;

@RestController
@RequestMapping("/api/v1/students/{studentId}/courses/{courseId}/quiz")
public class StudentQuizController {

    private final StudentQuizService studentQuizService;

    public StudentQuizController(StudentQuizService studentQuizService) {
        this.studentQuizService = studentQuizService;
    }

    @GetMapping("/status")
    public StudentQuizStatusDTO getQuizStatus(@AuthenticationPrincipal String authenticatedStudentId,
                                               @PathVariable String studentId,
                                               @PathVariable String courseId) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Access denied");
        }
        return studentQuizService.getQuizStatus(studentId, courseId);
    }
}
