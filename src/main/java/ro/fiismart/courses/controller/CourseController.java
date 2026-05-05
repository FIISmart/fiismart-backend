package ro.fiismart.courses.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.courses.dto.request.CreateCourseRequest;
import ro.fiismart.courses.dto.request.UpdateCourseRequest;
import ro.fiismart.courses.dto.response.CourseResponse;
import ro.fiismart.courses.service.CourseManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseManagementService courseService;

    public CourseController(CourseManagementService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable String id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getCourses(
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) Boolean published) {
        if (teacherId != null) return ResponseEntity.ok(courseService.getCoursesByTeacherId(teacherId));
        if (Boolean.TRUE.equals(published)) return ResponseEntity.ok(courseService.getPublishedCourses());
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable String id,
                                                        @Valid @RequestBody UpdateCourseRequest req) {
        return ResponseEntity.ok(courseService.updateCourse(id, req));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<CourseResponse> publish(@PathVariable String id) {
        return ResponseEntity.ok(courseService.publishCourse(id));
    }

    @PatchMapping("/{id}/draft")
    public ResponseEntity<CourseResponse> draft(@PathVariable String id) {
        return ResponseEntity.ok(courseService.draftCourse(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}
