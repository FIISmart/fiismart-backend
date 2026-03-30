package com.app.controller;

import com.app.dto.request.QuizAttemptRequest;
import com.app.dto.response.QuizAttemptResponse;
import com.app.service.QuizAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz-attempts")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @PostMapping
    public ResponseEntity<QuizAttemptResponse> create(@Valid @RequestBody QuizAttemptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizAttemptService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizAttemptResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(quizAttemptService.findById(id));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<QuizAttemptResponse>> findByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(quizAttemptService.findByStudentId(studentId));
    }

    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<List<QuizAttemptResponse>> findByQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(quizAttemptService.findByQuizId(quizId));
    }

    @GetMapping("/student/{studentId}/quiz/{quizId}")
    public ResponseEntity<List<QuizAttemptResponse>> findByStudentAndQuiz(
            @PathVariable String studentId,
            @PathVariable String quizId) {
        return ResponseEntity.ok(quizAttemptService.findByStudentAndQuiz(studentId, quizId));
    }

    @GetMapping("/student/{studentId}/quiz/{quizId}/latest")
    public ResponseEntity<QuizAttemptResponse> findLatest(
            @PathVariable String studentId,
            @PathVariable String quizId) {
        return ResponseEntity.ok(quizAttemptService.findLatestAttempt(studentId, quizId));
    }

    @GetMapping("/quiz/{quizId}/passed")
    public ResponseEntity<List<QuizAttemptResponse>> findPassedByQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(quizAttemptService.findPassedByQuiz(quizId));
    }

    @GetMapping("/quiz/{quizId}/avg-score")
    public ResponseEntity<Map<String, Double>> computeAvgScore(@PathVariable String quizId) {
        return ResponseEntity.ok(Map.of("avgScore", quizAttemptService.computeAvgScore(quizId)));
    }

    @GetMapping("/student/{studentId}/quiz/{quizId}/passed")
    public ResponseEntity<Map<String, Boolean>> hasStudentPassed(
            @PathVariable String studentId,
            @PathVariable String quizId) {
        return ResponseEntity.ok(Map.of("passed", quizAttemptService.hasStudentPassedQuiz(studentId, quizId)));
    }

    @GetMapping("/count/quiz/{quizId}/passed")
    public ResponseEntity<Map<String, Long>> countPassedByQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(Map.of("count", quizAttemptService.countPassedByQuiz(quizId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        quizAttemptService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
