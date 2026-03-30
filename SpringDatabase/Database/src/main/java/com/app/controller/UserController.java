package com.app.controller;

import com.app.dto.request.UserRequest;
import com.app.dto.response.UserResponse;
import com.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> findByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponse>> findByRole(@PathVariable String role) {
        return ResponseEntity.ok(userService.findAllByRole(role));
    }

    @GetMapping("/teachers")
    public ResponseEntity<List<UserResponse>> findAllTeachers() {
        return ResponseEntity.ok(userService.findAllTeachers());
    }

    @GetMapping("/students")
    public ResponseEntity<List<UserResponse>> findAllStudents() {
        return ResponseEntity.ok(userService.findAllStudents());
    }

    @GetMapping("/banned")
    public ResponseEntity<List<UserResponse>> findBanned() {
        return ResponseEntity.ok(userService.findBannedUsers());
    }

    @GetMapping("/enrolled-in/{courseId}")
    public ResponseEntity<List<UserResponse>> findStudentsEnrolledInCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(userService.findStudentsEnrolledInCourse(courseId));
    }

    @PatchMapping("/{id}/display-name")
    public ResponseEntity<Void> updateDisplayName(@PathVariable String id, @RequestBody Map<String, String> body) {
        userService.updateDisplayName(id, body.get("displayName"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ban")
    public ResponseEntity<Void> banUser(@PathVariable String id, @RequestBody Map<String, String> body) {
        userService.banUser(id, body.get("bannedBy"), body.get("reason"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable String id) {
        userService.unbanUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{teacherId}/owned-courses/{courseId}")
    public ResponseEntity<Void> addOwnedCourse(@PathVariable String teacherId, @PathVariable String courseId) {
        userService.addOwnedCourse(teacherId, courseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{teacherId}/owned-courses/{courseId}")
    public ResponseEntity<Void> removeOwnedCourse(@PathVariable String teacherId, @PathVariable String courseId) {
        userService.removeOwnedCourse(teacherId, courseId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{studentId}/enrolled-courses/{courseId}")
    public ResponseEntity<Void> addEnrolledCourse(@PathVariable String studentId, @PathVariable String courseId) {
        userService.addEnrolledCourse(studentId, courseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{studentId}/enrolled-courses/{courseId}")
    public ResponseEntity<Void> removeEnrolledCourse(@PathVariable String studentId, @PathVariable String courseId) {
        userService.removeEnrolledCourse(studentId, courseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/email/{email}")
    public ResponseEntity<Map<String, Boolean>> existsByEmail(@PathVariable String email) {
        return ResponseEntity.ok(Map.of("exists", userService.existsByEmail(email)));
    }

    @GetMapping("/count/role/{role}")
    public ResponseEntity<Map<String, Long>> countByRole(@PathVariable String role) {
        return ResponseEntity.ok(Map.of("count", userService.countByRole(role)));
    }
}
