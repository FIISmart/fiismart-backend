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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizManagementService quizService;

    @PostMapping
    public ResponseEntity<QuizResponse> create(@Valid @RequestBody QuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(quizService.findById(id));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<QuizResponse> findByCourseId(@PathVariable String courseId) {
        return ResponseEntity.ok(quizService.findByCourseId(courseId));
    }

    @GetMapping("/{quizId}/questions")
    public ResponseEntity<List<QuizQuestionResponse>> findQuestions(@PathVariable String quizId) {
        return ResponseEntity.ok(quizService.findQuestions(quizId));
    }

    @PatchMapping("/{id}/title")
    public ResponseEntity<Void> updateTitle(@PathVariable String id, @RequestBody Map<String, String> body) {
        quizService.updateTitle(id, body.get("title"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/passing-score")
    public ResponseEntity<Void> updatePassingScore(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        quizService.updatePassingScore(id, body.get("passingScore"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/time-limit")
    public ResponseEntity<Void> updateTimeLimit(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        quizService.updateTimeLimit(id, body.get("timeLimit"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{quizId}/questions")
    public ResponseEntity<QuizQuestionResponse> addQuestion(
            @PathVariable String quizId,
            @Valid @RequestBody QuizQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.addQuestion(quizId, request));
    }

    @DeleteMapping("/{quizId}/questions/{questionId}")
    public ResponseEntity<Void> removeQuestion(@PathVariable String quizId, @PathVariable String questionId) {
        quizService.removeQuestion(quizId, questionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        quizService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/course/{courseId}")
    public ResponseEntity<Void> deleteByCourseId(@PathVariable String courseId) {
        quizService.deleteByCourseId(courseId);
        return ResponseEntity.noContent().build();
    }
}
