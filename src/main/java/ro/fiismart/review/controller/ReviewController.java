package ro.fiismart.review.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.util.AuthUtils;
import ro.fiismart.review.dto.ReviewRequest;
import ro.fiismart.review.dto.ReviewResponse;
import ro.fiismart.review.dto.UpdateReviewRequest;
import ro.fiismart.review.service.ReviewService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody ReviewRequest request) {
        request.setStudentId(AuthUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ReviewResponse> findByStudentAndCourse(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own reviews");
        }
        return ResponseEntity.ok(reviewService.findByStudentAndCourse(studentId, courseId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ReviewResponse>> findByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(reviewService.findByCourseId(courseId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ReviewResponse>> findByStudent(@PathVariable String studentId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own reviews");
        }
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
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> updateReview(@PathVariable String id, @Valid @RequestBody UpdateReviewRequest req) {
        ReviewResponse review = reviewService.findById(id);
        if (!review.getStudentId().equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only update your own reviews");
        }
        reviewService.updateReview(id, req.getStars(), req.getBody());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/soft-delete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> softDelete(@PathVariable String id) {
        ReviewResponse review = reviewService.findById(id);
        if (!review.getStudentId().equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only delete your own reviews");
        }
        reviewService.softDelete(id, AuthUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        ReviewResponse review = reviewService.findById(id);
        if (!review.getStudentId().equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only delete your own reviews");
        }
        reviewService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/course/{courseId}")
    public ResponseEntity<Map<String, Long>> countByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(Map.of("count", reviewService.countByCourse(courseId)));
    }

    @GetMapping("/exists/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Boolean>> hasReviewed(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only check your own reviews");
        }
        return ResponseEntity.ok(Map.of("reviewed", reviewService.hasStudentReviewedCourse(studentId, courseId)));
    }
}