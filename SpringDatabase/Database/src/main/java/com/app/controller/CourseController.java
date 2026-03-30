package com.app.controller;

import com.app.dto.request.CourseRequest;
import com.app.dto.request.LectureRequest;
import com.app.dto.response.CourseResponse;
import com.app.dto.response.LectureResponse;
import com.app.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(courseService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> findAll() {
        return ResponseEntity.ok(courseService.findAll());
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<CourseResponse>> findByTeacher(@PathVariable String teacherId) {
        return ResponseEntity.ok(courseService.findByTeacherId(teacherId));
    }

    @GetMapping("/published")
    public ResponseEntity<List<CourseResponse>> findPublishedVisible() {
        return ResponseEntity.ok(courseService.findPublishedVisible());
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<CourseResponse>> findByTag(@PathVariable String tag) {
        return ResponseEntity.ok(courseService.findByTag(tag));
    }

    @GetMapping("/min-rating/{minRating}")
    public ResponseEntity<List<CourseResponse>> findByMinRating(@PathVariable double minRating) {
        return ResponseEntity.ok(courseService.findByMinRating(minRating));
    }

    @GetMapping("/{courseId}/lectures")
    public ResponseEntity<List<LectureResponse>> findLectures(@PathVariable String courseId) {
        return ResponseEntity.ok(courseService.findLecturesByCourseId(courseId));
    }

    @PatchMapping("/{id}/title")
    public ResponseEntity<Void> updateTitle(@PathVariable String id, @RequestBody Map<String, String> body) {
        courseService.updateTitle(id, body.get("title"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        courseService.updateStatus(id, body.get("status"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/hidden")
    public ResponseEntity<Void> setHidden(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        courseService.setHidden(id, body.get("hidden"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/quiz/{quizId}")
    public ResponseEntity<Void> setQuizId(@PathVariable String id, @PathVariable String quizId) {
        courseService.setQuizId(id, quizId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/lectures")
    public ResponseEntity<LectureResponse> addLecture(
            @PathVariable String courseId,
            @Valid @RequestBody LectureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.addLecture(courseId, request));
    }

    @DeleteMapping("/{courseId}/lectures/{lectureId}")
    public ResponseEntity<Void> removeLecture(@PathVariable String courseId, @PathVariable String lectureId) {
        courseService.removeLecture(courseId, lectureId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        courseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/teacher/{teacherId}")
    public ResponseEntity<Map<String, Long>> countByTeacher(@PathVariable String teacherId) {
        return ResponseEntity.ok(Map.of("count", courseService.countByTeacher(teacherId)));
    }
}
