package ro.fiismart.quiz.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.quiz.dto.QuizQuestionRequest;
import ro.fiismart.quiz.dto.QuizQuestionResponse;
import ro.fiismart.quiz.dto.QuizRequest;
import ro.fiismart.quiz.dto.QuizResponse;
import ro.fiismart.quiz.service.QuizManagementService;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/quiz")
@RequiredArgsConstructor
public class QuizBuilderController {

    private final QuizManagementService quizService;

    @GetMapping
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable String courseId) {
        return ResponseEntity.ok(quizService.findByCourseId(courseId));
    }

    @PostMapping
    public ResponseEntity<QuizResponse> createQuiz(
            @PathVariable String courseId,
            @Valid @RequestBody QuizRequest req) {
        req.setCourseId(courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.create(req));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteQuiz(@PathVariable String courseId) {
        quizService.deleteByCourseId(courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/questions")
    public ResponseEntity<QuizQuestionResponse> addQuestion(
            @PathVariable String courseId,
            @Valid @RequestBody QuizQuestionRequest req) {
        String quizId = quizService.findByCourseId(courseId).getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.addQuestion(quizId, req));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> removeQuestion(
            @PathVariable String courseId,
            @PathVariable String questionId) {
        String quizId = quizService.findByCourseId(courseId).getId();
        quizService.removeQuestion(quizId, questionId);
        return ResponseEntity.noContent().build();
    }
}
