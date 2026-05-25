package ro.fiismart.enrollment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.model.LectureProgressEntry;
import ro.fiismart.common.util.AuthUtils;
import ro.fiismart.enrollment.dto.EnrollmentRequest;
import ro.fiismart.enrollment.dto.EnrollmentResponse;
import ro.fiismart.enrollment.dto.UpdateEnrollmentStatusRequest;
import ro.fiismart.enrollment.dto.UpdateProgressRequest;
import ro.fiismart.enrollment.service.EnrollmentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> create(@Valid @RequestBody EnrollmentRequest request) {
        request.setStudentId(AuthUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.create(request));
    }

    @GetMapping("/me/{courseId}/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Boolean>> checkMyEnrollment(
            @PathVariable String courseId,
            @AuthenticationPrincipal String currentUserId) {
        return ResponseEntity.ok(Map.of("enrolled", enrollmentService.isEnrolled(currentUserId, courseId)));
    }

    @PostMapping("/me/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, String>> enrollMe(
            @PathVariable String courseId,
            @AuthenticationPrincipal String currentUserId) {
        EnrollmentRequest req = new EnrollmentRequest();
        req.setStudentId(currentUserId);
        req.setCourseId(courseId);
        EnrollmentResponse resp = enrollmentService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", resp.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(enrollmentService.findById(id));
    }

    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> findByStudentAndCourse(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own enrollments");
        }
        return ResponseEntity.ok(enrollmentService.findByStudentAndCourse(studentId, courseId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<EnrollmentResponse>> findByStudent(@PathVariable String studentId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own enrollments");
        }
        return ResponseEntity.ok(enrollmentService.findByStudentId(studentId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<EnrollmentResponse>> findByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(enrollmentService.findByCourseId(courseId));
    }

    @GetMapping("/student/{studentId}/completed")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<EnrollmentResponse>> findCompleted(@PathVariable String studentId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own enrollments");
        }
        return ResponseEntity.ok(enrollmentService.findCompletedByStudent(studentId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> updateStatus(@PathVariable String id, @Valid @RequestBody UpdateEnrollmentStatusRequest req) {
        EnrollmentResponse enrollment = enrollmentService.findById(id);
        if (!enrollment.getStudentId().equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only update your own enrollments");
        }
        enrollmentService.updateStatus(id, req.getStatus());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> updateProgress(@PathVariable String id, @Valid @RequestBody UpdateProgressRequest req) {
        EnrollmentResponse enrollment = enrollmentService.findById(id);
        if (!enrollment.getStudentId().equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only update your own enrollments");
        }
        enrollmentService.updateOverallProgress(id, req.getProgress());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> markCompleted(@PathVariable String id) {
        EnrollmentResponse enrollment = enrollmentService.findById(id);
        if (!enrollment.getStudentId().equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only update your own enrollments");
        }
        enrollmentService.markCompleted(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lecture-progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> addLectureProgress(
            @PathVariable String id,
            @Valid @RequestBody LectureProgressEntry entry) {
        EnrollmentResponse enrollment = enrollmentService.findById(id);
        if (!enrollment.getStudentId().equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only update your own enrollments");
        }
        enrollmentService.addLectureProgress(id, entry);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        EnrollmentResponse enrollment = enrollmentService.findById(id);
        if (!enrollment.getStudentId().equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only delete your own enrollments");
        }
        enrollmentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> deleteByStudentAndCourse(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only delete your own enrollments");
        }
        enrollmentService.deleteByStudentAndCourse(studentId, courseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Boolean>> isEnrolled(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only check your own enrollments");
        }
        return ResponseEntity.ok(Map.of("enrolled", enrollmentService.isEnrolled(studentId, courseId)));
    }

    @GetMapping("/count/course/{courseId}")
    public ResponseEntity<Map<String, Long>> countByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(Map.of("count", enrollmentService.countByCourse(courseId)));
    }
}