package ro.fiismart.moderation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.moderation.dto.CommentModerationResponse;
import ro.fiismart.moderation.service.CommentModerationService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommentModerationController {

    private final CommentModerationService commentModerationService;

    @GetMapping("/api/v1/courses/{courseId}/comments")
    public ResponseEntity<List<CommentModerationResponse>> getComments(@PathVariable String courseId) {
        return ResponseEntity.ok(commentModerationService.getCommentsByCourseId(courseId));
    }

    @PatchMapping("/api/v1/comments/{commentId}/status")
    public ResponseEntity<Void> updateCommentStatus(
            @PathVariable String commentId,
            @RequestBody Map<String, String> body) {
        commentModerationService.updateCommentStatus(commentId, body.get("status"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable String commentId) {
        commentModerationService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
