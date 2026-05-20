package ro.fiismart.quizattempt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.quizattempt.dto.QuizAttemptRequest;
import ro.fiismart.quizattempt.dto.QuizAttemptResponse;
import ro.fiismart.quizattempt.dto.StartQuizAttemptRequest;
import ro.fiismart.quizattempt.dto.StartQuizAttemptResponse;
import ro.fiismart.quizattempt.dto.SubmitQuizAttemptRequest;
import ro.fiismart.quizattempt.service.QuizAttemptService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/quiz-attempts")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @PostMapping
    public ResponseEntity<QuizAttemptResponse> create(@Valid @RequestBody QuizAttemptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizAttemptService.create(request));
    }

    /**
     * Begin a timed attempt. Idempotent: a second call while an attempt is
     * already {@code IN_PROGRESS} returns the same attempt instead of spawning
     * a duplicate. Student-gated — never trust the client to identify itself.
     */
    @PostMapping("/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StartQuizAttemptResponse> start(
            @Valid @RequestBody StartQuizAttemptRequest req,
            @AuthenticationPrincipal String studentId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizAttemptService.startAttempt(studentId, req.quizId()));
    }

    /**
     * Finalize an attempt. The service grades the answers server-side; the
     * request body deliberately has no score/passed fields.
     */
    @PostMapping("/{attemptId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizAttemptResponse> submit(
            @PathVariable String attemptId,
            @Valid @RequestBody SubmitQuizAttemptRequest req,
            @AuthenticationPrincipal String studentId) {
        return ResponseEntity.ok(quizAttemptService.submitAttempt(studentId, attemptId, req.answers()));
    }

    /** Flag an in-progress attempt as abandoned. Idempotent for terminal states. */
    @PostMapping("/{attemptId}/abandon")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> abandon(
            @PathVariable String attemptId,
            @AuthenticationPrincipal String studentId) {
        quizAttemptService.abandonAttempt(studentId, attemptId);
        return ResponseEntity.noContent().build();
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
