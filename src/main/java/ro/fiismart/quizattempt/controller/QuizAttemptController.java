package ro.fiismart.quizattempt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.util.AuthUtils;
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
    @Deprecated
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizAttemptResponse> create(@Valid @RequestBody QuizAttemptRequest request) {
        request.setStudentId(AuthUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(quizAttemptService.create(request));
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StartQuizAttemptResponse> start(
            @Valid @RequestBody StartQuizAttemptRequest req,
            @AuthenticationPrincipal String studentId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizAttemptService.startAttempt(studentId, req.quizId()));
    }

    @PostMapping("/{attemptId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizAttemptResponse> submit(
            @PathVariable String attemptId,
            @Valid @RequestBody SubmitQuizAttemptRequest req,
            @AuthenticationPrincipal String studentId) {
        return ResponseEntity.ok(quizAttemptService.submitAttempt(studentId, attemptId, req.answers()));
    }

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
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<QuizAttemptResponse>> findByStudent(@PathVariable String studentId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own quiz attempts");
        }
        return ResponseEntity.ok(quizAttemptService.findByStudentId(studentId));
    }

    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<List<QuizAttemptResponse>> findByQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(quizAttemptService.findByQuizId(quizId));
    }

    @GetMapping("/student/{studentId}/quiz/{quizId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<QuizAttemptResponse>> findByStudentAndQuiz(
            @PathVariable String studentId,
            @PathVariable String quizId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own quiz attempts");
        }
        return ResponseEntity.ok(quizAttemptService.findByStudentAndQuiz(studentId, quizId));
    }

    @GetMapping("/student/{studentId}/quiz/{quizId}/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizAttemptResponse> findLatest(
            @PathVariable String studentId,
            @PathVariable String quizId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own quiz attempts");
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
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Boolean>> hasStudentPassed(
            @PathVariable String studentId,
            @PathVariable String quizId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own quiz attempts");
        }
        return ResponseEntity.ok(Map.of("passed", quizAttemptService.hasStudentPassedQuiz(studentId, quizId)));
    }

    @GetMapping("/count/quiz/{quizId}/passed")
    public ResponseEntity<Map<String, Long>> countPassedByQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(Map.of("count", quizAttemptService.countPassedByQuiz(quizId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        QuizAttemptResponse attempt = quizAttemptService.findById(id);
        if (!attempt.getStudentId().equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only delete your own quiz attempts");
        }
        quizAttemptService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}