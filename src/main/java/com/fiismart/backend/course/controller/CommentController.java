package com.fiismart.backend.course.controller;

import com.fiismart.backend.course.dto.response.CommentResponse;
import com.fiismart.backend.course.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/courses/{courseId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable String courseId) {
        return ResponseEntity.ok(commentService.getCommentsByCourseId(courseId));
    }

    @PatchMapping("/comments/{commentId}/status")
    public ResponseEntity<Void> updateCommentStatus(
            @PathVariable String commentId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        commentService.updateCommentStatus(commentId, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable String commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
