package com.fiismart.backend.course.controller;

import com.fiismart.backend.course.dto.request.CreateQuizRequest;
import com.fiismart.backend.course.dto.request.QuizQuestionRequest;
import com.fiismart.backend.course.dto.response.QuizResponse;
import com.fiismart.backend.course.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * QuizBuilderController – gestionează construirea quiz-ului unui curs.
 *
 * Base path: /api/courses/{courseId}/quiz
 *
 * Endpoints:
 *   GET    /api/courses/{courseId}/quiz              – obține quiz-ul
 *   POST   /api/courses/{courseId}/quiz              – crează/înlocuiește quiz
 *   DELETE /api/courses/{courseId}/quiz              – șterge quiz
 *   POST   /api/courses/{courseId}/quiz/questions    – adaugă o întrebare
 *   DELETE /api/courses/{courseId}/quiz/questions/{questionId}  – șterge o întrebare
 *   PUT    /api/courses/{courseId}/quiz/questions/reorder       – reordonează întrebări
 */
@RestController
@RequestMapping("/api/courses/{courseId}/quiz")
public class QuizBuilderController {

    private final QuizService quizService;

    public QuizBuilderController(QuizService quizService) {
        this.quizService = quizService;
    }

    /** GET /api/courses/{courseId}/quiz */
    @GetMapping
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable String courseId) {
        return ResponseEntity.ok(quizService.getQuizByCourseId(courseId));
    }

    /**
     * POST /api/courses/{courseId}/quiz
     * Crează sau înlocuiește complet quiz-ul unui curs.
     */
    @PostMapping
    public ResponseEntity<QuizResponse> createOrReplaceQuiz(
            @PathVariable String courseId,
            @Valid @RequestBody CreateQuizRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizService.createOrUpdateQuiz(courseId, req));
    }

    /** DELETE /api/courses/{courseId}/quiz */
    @DeleteMapping
    public ResponseEntity<Void> deleteQuiz(@PathVariable String courseId) {
        quizService.deleteQuiz(courseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/courses/{courseId}/quiz/questions
     * Adaugă o singură întrebare la quiz-ul existent.
     */
    @PostMapping("/questions")
    public ResponseEntity<QuizResponse> addQuestion(
            @PathVariable String courseId,
            @Valid @RequestBody QuizQuestionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizService.addQuestion(courseId, req));
    }

    /**
     * DELETE /api/courses/{courseId}/quiz/questions/{questionId}
     * Șterge o întrebare din quiz.
     */
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> removeQuestion(
            @PathVariable String courseId,
            @PathVariable String questionId) {
        quizService.removeQuestion(courseId, questionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/courses/{courseId}/quiz/questions/reorder
     * Body: ["questionId1", "questionId2", ...] – în noua ordine (pentru drag & drop)
     */
    @PutMapping("/questions/reorder")
    public ResponseEntity<QuizResponse> reorderQuestions(
            @PathVariable String courseId,
            @RequestBody List<String> orderedQuestionIds) {
        return ResponseEntity.ok(quizService.reorderQuestions(courseId, orderedQuestionIds));
    }
}
