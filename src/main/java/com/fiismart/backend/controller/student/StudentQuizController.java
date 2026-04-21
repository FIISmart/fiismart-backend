package com.fiismart.backend.controller.student;

import com.fiismart.backend.dto.student.StudentQuizStatusDTO;
import com.fiismart.backend.service.student.StudentQuizService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students/{studentId}/courses/{courseId}/quiz")
@CrossOrigin(origins = "*")
public class StudentQuizController {

    private final StudentQuizService studentQuizService;

    public StudentQuizController(StudentQuizService studentQuizService) {
        this.studentQuizService = studentQuizService;
    }

    @GetMapping("/status")
    public StudentQuizStatusDTO getQuizStatus(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        return studentQuizService.getQuizStatus(studentId, courseId);
    }
}