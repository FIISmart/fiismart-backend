package ro.fiismart.tutors.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.fiismart.tutors.dto.TutorRequestCreateRequest;
import ro.fiismart.tutors.dto.TutorRequestResponse;
import ro.fiismart.tutors.dto.TutorRequestStatusUpdateRequest;
import ro.fiismart.tutors.service.TutorRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tutor-requests")
public class TutorRequestController {

    private final TutorRequestService tutorRequestService;

    public TutorRequestController(TutorRequestService tutorRequestService) {
        this.tutorRequestService = tutorRequestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TutorRequestResponse> create(
            @AuthenticationPrincipal String studentId,
            @Valid @RequestBody TutorRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tutorRequestService.create(studentId, request));
    }

    @GetMapping("/professor/me")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<List<TutorRequestResponse>> listForProfessor(@AuthenticationPrincipal String professorId) {
        return ResponseEntity.ok(tutorRequestService.listForProfessor(professorId));
    }

    @GetMapping("/student/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<TutorRequestResponse>> listForStudent(@AuthenticationPrincipal String studentId) {
        return ResponseEntity.ok(tutorRequestService.listForStudent(studentId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<TutorRequestResponse> updateStatus(
            @AuthenticationPrincipal String professorId,
            @PathVariable String id,
            @Valid @RequestBody TutorRequestStatusUpdateRequest request) {
        return ResponseEntity.ok(tutorRequestService.updateStatus(professorId, id, request));
    }
}
