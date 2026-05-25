package ro.fiismart.courses.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.courses.dto.request.CreateLectureRequest;
import ro.fiismart.courses.dto.request.CreateModuleRequest;
import ro.fiismart.courses.dto.request.UpdateLectureRequest;
import ro.fiismart.courses.dto.response.LectureResponse;
import ro.fiismart.courses.dto.response.ModuleResponse;
import ro.fiismart.courses.service.CourseManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/builder")
public class CourseBuilderController {

    private final CourseManagementService courseService;

    public CourseBuilderController(CourseManagementService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/modules")
    public ResponseEntity<List<ModuleResponse>> getModules(@PathVariable String courseId) {
        return ResponseEntity.ok(courseService.getModules(courseId));
    }

@PostMapping("/modules")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ModuleResponse> addModule(@PathVariable String courseId,
                                                     @Valid @RequestBody CreateModuleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.addModule(courseId, req));
    }

    @PutMapping("/modules/{moduleId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ModuleResponse> updateModule(@PathVariable String courseId,
                                                        @PathVariable String moduleId,
                                                        @Valid @RequestBody CreateModuleRequest req) {
        return ResponseEntity.ok(courseService.updateModule(courseId, moduleId, req));
    }

    @DeleteMapping("/modules/{moduleId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Void> deleteModule(@PathVariable String courseId,
                                              @PathVariable String moduleId) {
        courseService.deleteModule(courseId, moduleId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/modules/reorder")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<List<ModuleResponse>> reorderModules(@PathVariable String courseId,
                                                                @RequestBody List<String> orderedIds) {
        return ResponseEntity.ok(courseService.reorderModules(courseId, orderedIds));
    }

    @PostMapping("/modules/{moduleId}/lectures")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<LectureResponse> addLectureToModule(@PathVariable String courseId,
                                                               @PathVariable String moduleId,
                                                               @Valid @RequestBody CreateLectureRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.addLectureToModule(courseId, moduleId, req));
    }

    @PutMapping("/modules/{moduleId}/lectures/{lectureId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<LectureResponse> updateLectureInModule(@PathVariable String courseId,
                                                                     @PathVariable String moduleId,
                                                                     @PathVariable String lectureId,
                                                                     @Valid @RequestBody UpdateLectureRequest req) {
        return ResponseEntity.ok(courseService.updateLectureInModule(courseId, moduleId, lectureId, req));
    }

    @DeleteMapping("/modules/{moduleId}/lectures/{lectureId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Void> removeLectureFromModule(@PathVariable String courseId,
                                                         @PathVariable String moduleId,
                                                         @PathVariable String lectureId) {
        courseService.removeLectureFromModule(courseId, moduleId, lectureId);
        return ResponseEntity.noContent().build();
    }
}
