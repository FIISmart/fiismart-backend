package ro.fiismart.quiz.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.fiismart.quiz.dto.modulequiz.CreateModuleQuizRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizQuestionRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizResponse;
import ro.fiismart.quiz.service.ModuleQuizService;

import java.util.List;

/**
 * Endpoints for the new quiz builder UI: lecture / module / course-final.
 *
 * <pre>
 *   /api/v1/courses/{courseId}/builder/quizzes                                     (all)
 *
 *   /api/v1/courses/{courseId}/builder/modules/{moduleId}/lectures/{lectureId}/quiz
 *     ... + /questions, /questions/{questionId}, /questions/reorder
 *
 *   /api/v1/courses/{courseId}/builder/modules/{moduleId}/quiz
 *     ... + /questions, /questions/{questionId}, /questions/reorder
 *
 *   /api/v1/courses/{courseId}/builder/final-quiz
 *     ... + /questions, /questions/{questionId}, /questions/reorder
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/builder")
@RequiredArgsConstructor
public class ModuleQuizController {

    private final ModuleQuizService moduleQuizService;

    // ── ALL QUIZZES FOR A COURSE ─────────────────────────────────────────────

    @GetMapping("/quizzes")
    public ResponseEntity<List<ModuleQuizResponse>> getAllQuizzes(
            @PathVariable String courseId) {
        return ResponseEntity.ok(moduleQuizService.getAllQuizzesByCourse(courseId));
    }

    // ── LECTURE QUIZ ─────────────────────────────────────────────────────────

    @GetMapping("/modules/{moduleId}/lectures/{lectureId}/quiz")
    public ResponseEntity<ModuleQuizResponse> getLectureQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @PathVariable String lectureId) {
        return ResponseEntity.ok(moduleQuizService.getLectureQuiz(courseId, moduleId, lectureId));
    }

    @PostMapping("/modules/{moduleId}/lectures/{lectureId}/quiz")
    public ResponseEntity<ModuleQuizResponse> createOrReplaceLectureQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @PathVariable String lectureId,
            @Valid @RequestBody CreateModuleQuizRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moduleQuizService.createOrUpdateLectureQuiz(courseId, moduleId, lectureId, req));
    }

    @DeleteMapping("/modules/{moduleId}/lectures/{lectureId}/quiz")
    public ResponseEntity<Void> deleteLectureQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @PathVariable String lectureId) {
        moduleQuizService.deleteLectureQuiz(courseId, moduleId, lectureId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/modules/{moduleId}/lectures/{lectureId}/quiz/questions")
    public ResponseEntity<ModuleQuizResponse> addQuestionToLectureQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @PathVariable String lectureId,
            @Valid @RequestBody ModuleQuizQuestionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moduleQuizService.addQuestionToLectureQuiz(courseId, moduleId, lectureId, req));
    }

    @DeleteMapping("/modules/{moduleId}/lectures/{lectureId}/quiz/questions/{questionId}")
    public ResponseEntity<Void> removeQuestionFromLectureQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @PathVariable String lectureId,
            @PathVariable String questionId) {
        moduleQuizService.removeQuestionFromLectureQuiz(courseId, moduleId, lectureId, questionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/modules/{moduleId}/lectures/{lectureId}/quiz/questions/reorder")
    public ResponseEntity<ModuleQuizResponse> reorderLectureQuizQuestions(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @PathVariable String lectureId,
            @RequestBody List<String> orderedQuestionIds) {
        return ResponseEntity.ok(
                moduleQuizService.reorderLectureQuizQuestions(
                        courseId, moduleId, lectureId, orderedQuestionIds));
    }

    // ── MODULE QUIZ ──────────────────────────────────────────────────────────

    @GetMapping("/modules/{moduleId}/quiz")
    public ResponseEntity<ModuleQuizResponse> getModuleQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId) {
        return ResponseEntity.ok(moduleQuizService.getModuleQuiz(courseId, moduleId));
    }

    @PostMapping("/modules/{moduleId}/quiz")
    public ResponseEntity<ModuleQuizResponse> createOrReplaceModuleQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @Valid @RequestBody CreateModuleQuizRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moduleQuizService.createOrUpdateModuleQuiz(courseId, moduleId, req));
    }

    @DeleteMapping("/modules/{moduleId}/quiz")
    public ResponseEntity<Void> deleteModuleQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId) {
        moduleQuizService.deleteModuleQuiz(courseId, moduleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/modules/{moduleId}/quiz/questions")
    public ResponseEntity<ModuleQuizResponse> addQuestionToModuleQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @Valid @RequestBody ModuleQuizQuestionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moduleQuizService.addQuestionToModuleQuiz(courseId, moduleId, req));
    }

    @DeleteMapping("/modules/{moduleId}/quiz/questions/{questionId}")
    public ResponseEntity<Void> removeQuestionFromModuleQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @PathVariable String questionId) {
        moduleQuizService.removeQuestionFromModuleQuiz(courseId, moduleId, questionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/modules/{moduleId}/quiz/questions/reorder")
    public ResponseEntity<ModuleQuizResponse> reorderModuleQuizQuestions(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @RequestBody List<String> orderedQuestionIds) {
        return ResponseEntity.ok(
                moduleQuizService.reorderModuleQuizQuestions(
                        courseId, moduleId, orderedQuestionIds));
    }

    // ── COURSE-FINAL QUIZ ────────────────────────────────────────────────────

    @GetMapping("/final-quiz")
    public ResponseEntity<ModuleQuizResponse> getCourseFinalQuiz(
            @PathVariable String courseId) {
        return ResponseEntity.ok(moduleQuizService.getCourseFinalQuiz(courseId));
    }

    @PostMapping("/final-quiz")
    public ResponseEntity<ModuleQuizResponse> createOrReplaceCourseFinalQuiz(
            @PathVariable String courseId,
            @Valid @RequestBody CreateModuleQuizRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moduleQuizService.createOrUpdateCourseFinalQuiz(courseId, req));
    }

    @DeleteMapping("/final-quiz")
    public ResponseEntity<Void> deleteCourseFinalQuiz(
            @PathVariable String courseId) {
        moduleQuizService.deleteCourseFinalQuiz(courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/final-quiz/questions")
    public ResponseEntity<ModuleQuizResponse> addQuestionToCourseFinalQuiz(
            @PathVariable String courseId,
            @Valid @RequestBody ModuleQuizQuestionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moduleQuizService.addQuestionToCourseFinalQuiz(courseId, req));
    }

    @DeleteMapping("/final-quiz/questions/{questionId}")
    public ResponseEntity<Void> removeQuestionFromCourseFinalQuiz(
            @PathVariable String courseId,
            @PathVariable String questionId) {
        moduleQuizService.removeQuestionFromCourseFinalQuiz(courseId, questionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/final-quiz/questions/reorder")
    public ResponseEntity<ModuleQuizResponse> reorderCourseFinalQuizQuestions(
            @PathVariable String courseId,
            @RequestBody List<String> orderedQuestionIds) {
        return ResponseEntity.ok(
                moduleQuizService.reorderCourseFinalQuizQuestions(courseId, orderedQuestionIds));
    }
}
