package com.app.controller;

import com.app.dto.request.CommentRequest;
import com.app.dto.response.CommentResponse;
import com.app.model.ModerationFlag;
import com.app.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> create(@Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(commentService.findById(id));
    }

    @GetMapping("/lecture/{lectureId}")
    public ResponseEntity<List<CommentResponse>> findByLecture(@PathVariable String lectureId) {
        return ResponseEntity.ok(commentService.findByLectureId(lectureId));
    }

    @GetMapping("/lecture/{lectureId}/top-level")
    public ResponseEntity<List<CommentResponse>> findTopLevel(@PathVariable String lectureId) {
        return ResponseEntity.ok(commentService.findTopLevelByLectureId(lectureId));
    }

    @GetMapping("/{parentId}/replies")
    public ResponseEntity<List<CommentResponse>> findReplies(@PathVariable String parentId) {
        return ResponseEntity.ok(commentService.findRepliesByParentId(parentId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<CommentResponse>> findByAuthor(@PathVariable String authorId) {
        return ResponseEntity.ok(commentService.findByAuthorId(authorId));
    }

    @GetMapping("/flagged/{minFlags}")
    public ResponseEntity<List<CommentResponse>> findFlagged(@PathVariable int minFlags) {
        return ResponseEntity.ok(commentService.findFlagged(minFlags));
    }

    @PatchMapping("/{id}/body")
    public ResponseEntity<Void> updateBody(@PathVariable String id, @RequestBody Map<String, String> body) {
        commentService.updateBody(id, body.get("body"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/soft-delete")
    public ResponseEntity<Void> softDelete(@PathVariable String id) {
        commentService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable String id, @PathVariable String userId) {
        commentService.addLike(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable String id, @PathVariable String userId) {
        commentService.removeLike(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/flags")
    public ResponseEntity<Void> addFlag(@PathVariable String id, @RequestBody ModerationFlag flag) {
        commentService.addModerationFlag(id, flag);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/flags")
    public ResponseEntity<Void> clearFlags(@PathVariable String id) {
        commentService.clearModerationFlags(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        commentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/lecture/{lectureId}")
    public ResponseEntity<Map<String, Long>> countByLecture(@PathVariable String lectureId) {
        return ResponseEntity.ok(Map.of("count", commentService.countByLecture(lectureId)));
    }

    @GetMapping("/{id}/liked-by/{userId}")
    public ResponseEntity<Map<String, Boolean>> hasUserLiked(@PathVariable String id, @PathVariable String userId) {
        return ResponseEntity.ok(Map.of("liked", commentService.hasUserLiked(id, userId)));
    }
}
