package com.fiismart.backend.course.controller;

import com.fiismart.backend.course.dto.request.CreateCourseRequest;
import com.fiismart.backend.course.dto.request.UpdateCourseRequest;
import com.fiismart.backend.course.dto.response.CourseResponse;
import com.fiismart.backend.course.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        CourseResponse course = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable String id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getCourses(
            @RequestParam(required = false) String teacherId) {
        if (teacherId != null) {
            return ResponseEntity.ok(courseService.getCoursesByTeacherId(teacherId));
        }
        return ResponseEntity.ok(courseService.getPublishedCourses());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable String id,
            @Valid @RequestBody UpdateCourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<CourseResponse> publishCourse(@PathVariable String id) {
        return ResponseEntity.ok(courseService.publishCourse(id));
    }

    @PatchMapping("/{id}/draft")
    public ResponseEntity<CourseResponse> draftCourse(@PathVariable String id) {
        return ResponseEntity.ok(courseService.draftCourse(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}
