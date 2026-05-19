package ro.fiismart.quizattempt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizAttemptResponse> create(
            @AuthenticationPrincipal String studentId,
            @Valid @RequestBody QuizAttemptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizAttemptService.create(studentId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','PROFESSOR')")
    public ResponseEntity<QuizAttemptResponse> findById(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        QuizAttemptResponse attempt = quizAttemptService.findById(id);
        if (attempt != null && attempt.getStudentId() != null
                && !attempt.getStudentId().equals(userId)) {
            throw new AccessDeniedException("Cannot read another user's attempt");
        }
        return ResponseEntity.ok(attempt);
    }

    @GetMapping("/student/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<QuizAttemptResponse>> findMyAttempts(
            @AuthenticationPrincipal String studentId) {
        return ResponseEntity.ok(quizAttemptService.findByStudentId(studentId));
    }

    @GetMapping("/quiz/{quizId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<List<QuizAttemptResponse>> findByQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(quizAttemptService.findByQuizId(quizId));
    }

    @GetMapping("/student/me/quiz/{quizId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<QuizAttemptResponse>> findMyByQuiz(
            @AuthenticationPrincipal String studentId,
            @PathVariable String quizId) {
        return ResponseEntity.ok(quizAttemptService.findByStudentAndQuiz(studentId, quizId));
    }

    @GetMapping("/student/me/quiz/{quizId}/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizAttemptResponse> findMyLatest(
            @AuthenticationPrincipal String studentId,
            @PathVariable String quizId) {
        return ResponseEntity.ok(quizAttemptService.findLatestAttempt(studentId, quizId));
    }

    @GetMapping("/quiz/{quizId}/passed")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<List<QuizAttemptResponse>> findPassedByQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(quizAttemptService.findPassedByQuiz(quizId));
    }

    @GetMapping("/quiz/{quizId}/avg-score")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Map<String, Double>> computeAvgScore(@PathVariable String quizId) {
        return ResponseEntity.ok(Map.of("avgScore", quizAttemptService.computeAvgScore(quizId)));
    }

    @GetMapping("/student/me/quiz/{quizId}/passed")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Boolean>> hasStudentPassed(
            @AuthenticationPrincipal String studentId,
            @PathVariable String quizId) {
        return ResponseEntity.ok(Map.of("passed", quizAttemptService.hasStudentPassedQuiz(studentId, quizId)));
    }

    @GetMapping("/count/quiz/{quizId}/passed")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Map<String, Long>> countPassedByQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(Map.of("count", quizAttemptService.countPassedByQuiz(quizId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','PROFESSOR')")
    public ResponseEntity<Void> deleteById(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        QuizAttemptResponse attempt = quizAttemptService.findById(id);
        if (attempt != null && attempt.getStudentId() != null
                && !attempt.getStudentId().equals(userId)) {
            throw new AccessDeniedException("Cannot delete another user's attempt");
        }
        quizAttemptService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
