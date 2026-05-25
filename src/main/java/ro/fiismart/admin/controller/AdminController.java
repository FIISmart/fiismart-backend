package ro.fiismart.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.admin.dto.*;
import ro.fiismart.admin.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    // ── Users ──────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable String userId,
            @RequestBody AdminUpdateUserRequest req,
            @AuthenticationPrincipal String currentUserId) {
        return ResponseEntity.ok(adminService.updateUser(userId, req, currentUserId));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // ── Courses ────────────────────────────────────────────────────────

    @GetMapping("/courses")
    public ResponseEntity<List<AdminCourseResponse>> getAllCourses() {
        return ResponseEntity.ok(adminService.getAllCourses());
    }

    @PutMapping("/courses/{courseId}")
    public ResponseEntity<AdminCourseResponse> updateCourse(
            @PathVariable String courseId,
            @RequestBody AdminUpdateCourseRequest req) {
        return ResponseEntity.ok(adminService.updateCourse(courseId, req));
    }

    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String courseId) {
        adminService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }

    // ── Enrollments ────────────────────────────────────────────────────

    @GetMapping("/enrollments")
    public ResponseEntity<List<AdminEnrollmentResponse>> getAllEnrollments() {
        return ResponseEntity.ok(adminService.getAllEnrollments());
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable String enrollmentId) {
        adminService.deleteEnrollment(enrollmentId);
        return ResponseEntity.noContent().build();
    }
}
