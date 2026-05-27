package ro.fiismart.dashboard.student.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.fiismart.dashboard.student.dto.StudentPlayableQuizDTO;
import ro.fiismart.dashboard.student.service.StudentQuizService;

@RestController
@RequestMapping("/api/v1/student-quizzes")
public class StudentPlayableQuizController {

    private final StudentQuizService studentQuizService;

    public StudentPlayableQuizController(StudentQuizService studentQuizService) {
        this.studentQuizService = studentQuizService;
    }

    @GetMapping("/{quizId}")
    public StudentPlayableQuizDTO getPlayableQuiz(@PathVariable String quizId,
                                                  @AuthenticationPrincipal String userId) {
        return studentQuizService.getPlayableQuiz(userId, quizId);
    }
}
