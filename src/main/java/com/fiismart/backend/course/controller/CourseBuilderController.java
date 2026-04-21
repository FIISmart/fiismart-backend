package com.fiismart.backend.course.controller;

import com.fiismart.backend.course.dto.request.CreateLectureRequest;
import com.fiismart.backend.course.dto.request.CreateModuleRequest;
import com.fiismart.backend.course.dto.request.CreateQuizRequest;
import com.fiismart.backend.course.dto.request.UpdateLectureRequest;
import com.fiismart.backend.course.dto.response.LectureResponse;
import com.fiismart.backend.course.dto.response.ModuleResponse;
import com.fiismart.backend.course.dto.response.QuizResponse;
import com.fiismart.backend.course.service.LectureService;
import com.fiismart.backend.course.service.ModuleService;
import com.fiismart.backend.course.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CourseBuilderController – gestionează construirea unui curs:
 *   - Module (creare, editare, ștergere, reordonare)
 *   - Lecturi libere (fără modul)
 *   - Lecturi în modul (creare, editare, ștergere, reordonare)
 *
 * Base path: /api/courses/{courseId}/builder
 */
@RestController
@RequestMapping("/api/courses/{courseId}/builder")
public class CourseBuilderController {

    private final ModuleService moduleService;
    private final LectureService lectureService;
    private final QuizService quizService;

    public CourseBuilderController(ModuleService moduleService,
                                   LectureService lectureService,
                                   QuizService quizService) {
        this.moduleService = moduleService;
        this.lectureService = lectureService;
        this.quizService = quizService;
    }

    /** GET /api/courses/{courseId}/builder/modules */
    @GetMapping("/modules")
    public ResponseEntity<List<ModuleResponse>> getModules(@PathVariable String courseId) {
        return ResponseEntity.ok(moduleService.getModules(courseId));
    }

    /** POST /api/courses/{courseId}/builder/modules */
    @PostMapping("/modules")
    public ResponseEntity<ModuleResponse> addModule(
            @PathVariable String courseId,
            @Valid @RequestBody CreateModuleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moduleService.addModule(courseId, req));
    }

    /** PUT /api/courses/{courseId}/builder/modules/{moduleId} */
    @PutMapping("/modules/{moduleId}")
    public ResponseEntity<ModuleResponse> updateModule(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @RequestBody CreateModuleRequest req) {
        return ResponseEntity.ok(moduleService.updateModule(courseId, moduleId, req));
    }

    /** DELETE /api/courses/{courseId}/builder/modules/{moduleId} */
    @DeleteMapping("/modules/{moduleId}")
    public ResponseEntity<Void> deleteModule(
            @PathVariable String courseId,
            @PathVariable String moduleId) {
        moduleService.deleteModule(courseId, moduleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/courses/{courseId}/builder/modules/reorder
     * Body: ["moduleId1", "moduleId2", ...] – în noua ordine
     */
    @PutMapping("/modules/reorder")
    public ResponseEntity<List<ModuleResponse>> reorderModules(
            @PathVariable String courseId,
            @RequestBody List<String> orderedModuleIds) {
        return ResponseEntity.ok(moduleService.reorderModules(courseId, orderedModuleIds));
    }


    /** POST /api/courses/{courseId}/builder/modules/{moduleId}/lectures */
    @PostMapping("/modules/{moduleId}/lectures")
    public ResponseEntity<LectureResponse> addLectureToModule(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @Valid @RequestBody CreateLectureRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lectureService.addLectureToModule(courseId, moduleId, req));
    }

    /** PUT /api/courses/{courseId}/builder/modules/{moduleId}/lectures/{lectureId} */
    @PutMapping("/modules/{moduleId}/lectures/{lectureId}")
    public ResponseEntity<LectureResponse> updateLectureInModule(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @PathVariable String lectureId,
            @RequestBody UpdateLectureRequest req) {
        return ResponseEntity.ok(
                lectureService.updateLectureInModule(courseId, moduleId, lectureId, req));
    }

    /** DELETE /api/courses/{courseId}/builder/modules/{moduleId}/lectures/{lectureId} */
    @DeleteMapping("/modules/{moduleId}/lectures/{lectureId}")
    public ResponseEntity<Void> removeLectureFromModule(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @PathVariable String lectureId) {
        lectureService.removeLectureFromModule(courseId, moduleId, lectureId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/courses/{courseId}/builder/modules/{moduleId}/lectures/reorder
     * Body: ["lectureId1", "lectureId2", ...] – în noua ordine
     */
    @PutMapping("/modules/{moduleId}/lectures/reorder")
    public ResponseEntity<List<LectureResponse>> reorderLecturesInModule(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @RequestBody List<String> orderedLectureIds) {
        return ResponseEntity.ok(
                lectureService.reorderLecturesInModule(courseId, moduleId, orderedLectureIds));
    }


    /** GET /api/courses/{courseId}/builder/lectures */
    @GetMapping("/lectures")
    public ResponseEntity<List<LectureResponse>> getLectures(@PathVariable String courseId) {
        return ResponseEntity.ok(lectureService.getLecturesForCourse(courseId));
    }

    /** POST /api/courses/{courseId}/builder/lectures */
    @PostMapping("/lectures")
    public ResponseEntity<LectureResponse> addLecture(
            @PathVariable String courseId,
            @Valid @RequestBody CreateLectureRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lectureService.addLectureToCourse(courseId, req));
    }

    /** PUT /api/courses/{courseId}/builder/lectures/{lectureId} */
    @PutMapping("/lectures/{lectureId}")
    public ResponseEntity<LectureResponse> updateLecture(
            @PathVariable String courseId,
            @PathVariable String lectureId,
            @RequestBody UpdateLectureRequest req) {
        return ResponseEntity.ok(lectureService.updateLectureInCourse(courseId, lectureId, req));
    }

    /** DELETE /api/courses/{courseId}/builder/lectures/{lectureId} */
    @DeleteMapping("/lectures/{lectureId}")
    public ResponseEntity<Void> removeLecture(
            @PathVariable String courseId,
            @PathVariable String lectureId) {
        lectureService.removeLectureFromCourse(courseId, lectureId);
        return ResponseEntity.noContent().build();
    }


    // ── Module-scoped quiz ─────────────────────────────────────────────────

    /** GET /api/courses/{courseId}/builder/modules/{moduleId}/quiz */
    @GetMapping("/modules/{moduleId}/quiz")
    public ResponseEntity<QuizResponse> getModuleQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId) {
        return ResponseEntity.ok(quizService.getQuizByModuleId(courseId, moduleId));
    }

    /** POST /api/courses/{courseId}/builder/modules/{moduleId}/quiz */
    @PostMapping("/modules/{moduleId}/quiz")
    public ResponseEntity<QuizResponse> createOrUpdateModuleQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId,
            @Valid @RequestBody CreateQuizRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizService.createOrUpdateModuleQuiz(courseId, moduleId, req));
    }

    /** DELETE /api/courses/{courseId}/builder/modules/{moduleId}/quiz */
    @DeleteMapping("/modules/{moduleId}/quiz")
    public ResponseEntity<Void> deleteModuleQuiz(
            @PathVariable String courseId,
            @PathVariable String moduleId) {
        quizService.deleteModuleQuiz(courseId, moduleId);
        return ResponseEntity.noContent().build();
    }
}
