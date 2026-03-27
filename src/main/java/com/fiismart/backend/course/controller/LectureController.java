package com.fiismart.backend.course.controller;

import com.fiismart.backend.course.dto.request.CreateLectureRequest;
import com.fiismart.backend.course.dto.request.UpdateLectureRequest;
import com.fiismart.backend.course.dto.response.LectureResponse;
import com.fiismart.backend.course.service.LectureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/lectures")
public class LectureController {

    private final LectureService lectureService;

    public LectureController(LectureService lectureService) {
        this.lectureService = lectureService;
    }

    @GetMapping
    public ResponseEntity<List<LectureResponse>> getLectures(@PathVariable String courseId) {
        return ResponseEntity.ok(lectureService.getLectures(courseId));
    }

    @PostMapping
    public ResponseEntity<LectureResponse> addLecture(
            @PathVariable String courseId,
            @Valid @RequestBody CreateLectureRequest request) {
        LectureResponse lecture = lectureService.addLecture(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(lecture);
    }

    @PutMapping("/{lectureId}")
    public ResponseEntity<LectureResponse> updateLecture(
            @PathVariable String courseId,
            @PathVariable String lectureId,
            @Valid @RequestBody UpdateLectureRequest request) {
        return ResponseEntity.ok(lectureService.updateLecture(courseId, lectureId, request));
    }

    @DeleteMapping("/{lectureId}")
    public ResponseEntity<Void> deleteLecture(
            @PathVariable String courseId,
            @PathVariable String lectureId) {
        lectureService.deleteLecture(courseId, lectureId);
        return ResponseEntity.noContent().build();
    }
}
