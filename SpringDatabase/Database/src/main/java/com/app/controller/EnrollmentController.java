package com.app.controller;

import com.app.dto.request.EnrollmentRequest;
import com.app.dto.response.EnrollmentResponse;
import com.app.model.LectureProgress;
import com.app.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<EnrollmentResponse> create(@Valid @RequestBody EnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(enrollmentService.findById(id));
    }

    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<EnrollmentResponse> findByStudentAndCourse(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        return ResponseEntity.ok(enrollmentService.findByStudentAndCourse(studentId, courseId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<EnrollmentResponse>> findByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(enrollmentService.findByStudentId(studentId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<EnrollmentResponse>> findByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(enrollmentService.findByCourseId(courseId));
    }

    @GetMapping("/student/{studentId}/completed")
    public ResponseEntity<List<EnrollmentResponse>> findCompleted(@PathVariable String studentId) {
        return ResponseEntity.ok(enrollmentService.findCompletedByStudent(studentId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        enrollmentService.updateStatus(id, body.get("status"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/progress")
    public ResponseEntity<Void> updateProgress(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        enrollmentService.updateOverallProgress(id, body.get("overallProgress"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> markCompleted(@PathVariable String id) {
        enrollmentService.markCompleted(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lecture-progress")
    public ResponseEntity<Void> addLectureProgress(
            @PathVariable String id,
            @RequestBody LectureProgress lectureProgress) {
        enrollmentService.addLectureProgress(id, lectureProgress);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        enrollmentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<Void> deleteByStudentAndCourse(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        enrollmentService.deleteByStudentAndCourse(studentId, courseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/student/{studentId}/course/{courseId}")
    public ResponseEntity<Map<String, Boolean>> isEnrolled(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        return ResponseEntity.ok(Map.of("enrolled", enrollmentService.isEnrolled(studentId, courseId)));
    }

    @GetMapping("/count/course/{courseId}")
    public ResponseEntity<Map<String, Long>> countByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(Map.of("count", enrollmentService.countByCourse(courseId)));
    }
}
