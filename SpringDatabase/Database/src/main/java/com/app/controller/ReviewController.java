package com.app.controller;

import com.app.dto.request.ReviewRequest;
import com.app.dto.response.ReviewResponse;
import com.app.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<ReviewResponse> findByStudentAndCourse(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        return ResponseEntity.ok(reviewService.findByStudentAndCourse(studentId, courseId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ReviewResponse>> findByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(reviewService.findByCourseId(courseId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ReviewResponse>> findByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(reviewService.findByStudentId(studentId));
    }

    @GetMapping("/course/{courseId}/stars/{stars}")
    public ResponseEntity<List<ReviewResponse>> findByCourseAndStars(
            @PathVariable String courseId,
            @PathVariable int stars) {
        return ResponseEntity.ok(reviewService.findByCourseAndStars(courseId, stars));
    }

    @GetMapping("/course/{courseId}/avg-rating")
    public ResponseEntity<Map<String, Double>> computeAvgRating(@PathVariable String courseId) {
        return ResponseEntity.ok(Map.of("avgRating", reviewService.computeAvgRating(courseId)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateReview(@PathVariable String id, @RequestBody Map<String, Object> body) {
        reviewService.updateReview(id, (Integer) body.get("stars"), (String) body.get("body"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/soft-delete")
    public ResponseEntity<Void> softDelete(@PathVariable String id, @RequestBody Map<String, String> body) {
        reviewService.softDelete(id, body.get("deletedBy"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        reviewService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/course/{courseId}")
    public ResponseEntity<Map<String, Long>> countByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(Map.of("count", reviewService.countByCourse(courseId)));
    }
}
