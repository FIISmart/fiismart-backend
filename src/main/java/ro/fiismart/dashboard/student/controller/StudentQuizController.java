package ro.fiismart.dashboard.student.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.util.AuthUtils;
import ro.fiismart.dashboard.student.dto.StudentQuizStatusDTO;
import ro.fiismart.dashboard.student.service.StudentQuizService;

@RestController
@RequestMapping("/api/v1/students/{studentId}/courses/{courseId}/quiz")
@PreAuthorize("hasRole('STUDENT')")
public class StudentQuizController {

    private final StudentQuizService studentQuizService;

    public StudentQuizController(StudentQuizService studentQuizService) {
        this.studentQuizService = studentQuizService;
    }

    @GetMapping("/status")
    public StudentQuizStatusDTO getQuizStatus(@PathVariable String studentId,
                                               @PathVariable String courseId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own data");
        }
        return studentQuizService.getQuizStatus(studentId, courseId);
    }
}