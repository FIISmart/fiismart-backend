package com.fiismart.backend.course.controller;

import com.fiismart.backend.course.dto.request.CreateQuizRequest;
import com.fiismart.backend.course.dto.response.QuizResponse;
import com.fiismart.backend.course.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable String courseId) {
        return ResponseEntity.ok(quizService.getQuizByCourseId(courseId));
    }

    @PostMapping
    public ResponseEntity<QuizResponse> createOrUpdateQuiz(
            @PathVariable String courseId,
            @Valid @RequestBody CreateQuizRequest request) {
        QuizResponse quiz = quizService.createOrUpdateQuiz(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(quiz);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteQuiz(@PathVariable String courseId) {
        quizService.deleteQuiz(courseId);
        return ResponseEntity.noContent().build();
    }
}
