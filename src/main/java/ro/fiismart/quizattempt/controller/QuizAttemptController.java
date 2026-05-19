package ro.fiismart.quizattempt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.quizattempt.dto.QuizAttemptRequest;
import ro.fiismart.quizattempt.dto.QuizAttemptResponse;
import ro.fiismart.quizattempt.service.QuizAttemptService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/quiz-attempts")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @PostMapping
    public ResponseEntity<QuizAttemptResponse> create(@AuthenticationPrincipal String authenticatedStudentId,
                                                       @Valid @RequestBody QuizAttemptRequest request) {
        // Force the studentId to be the authenticated user
        request.setStudentId(authenticatedStudentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(quizAttemptService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizAttemptResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(quizAttemptService.findById(id));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<QuizAttemptResponse>> findByStudent(@AuthenticationPrincipal String authenticatedStudentId,
                                                                    @PathVariable String studentId) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Access denied to other student's attempts");
        }
        return ResponseEntity.ok(quizAttemptService.findByStudentId(studentId));
    }

    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<List<QuizAttemptResponse>> findByQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(quizAttemptService.findByQuizId(quizId));
    }

    @GetMapping("/student/{studentId}/quiz/{quizId}")
    public ResponseEntity<List<QuizAttemptResponse>> findByStudentAndQuiz(
            @AuthenticationPrincipal String authenticatedStudentId,
            @PathVariable String studentId,
            @PathVariable String quizId) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Access denied to other student's attempts");
        }
        return ResponseEntity.ok(quizAttemptService.findByStudentAndQuiz(studentId, quizId));
    }

    @GetMapping("/student/{studentId}/quiz/{quizId}/latest")
    public ResponseEntity<QuizAttemptResponse> findLatest(
            @AuthenticationPrincipal String authenticatedStudentId,
            @PathVariable String studentId,
            @PathVariable String quizId) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Access denied to other student's attempts");
        }
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
            @AuthenticationPrincipal String authenticatedStudentId,
            @PathVariable String studentId,
            @PathVariable String quizId) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Access denied");
        }
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
