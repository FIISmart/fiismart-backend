package ro.fiismart.dashboard.student.controller;

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
    public StudentQuizStatusDTO getQuizStatus(@PathVariable String studentId,
                                               @PathVariable String courseId) {
        return studentQuizService.getQuizStatus(studentId, courseId);
    }
}
