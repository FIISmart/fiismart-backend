package com.fiismart.backend.controller.student;

import com.fiismart.backend.dto.student.CommentCreateRequest;
import com.fiismart.backend.dto.student.StudentCommentDTO;
import com.fiismart.backend.service.student.StudentCommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students/{studentId}")
@CrossOrigin(origins = "*")
public class StudentCommentController {

    private final StudentCommentService commentService;

    public StudentCommentController(StudentCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/courses/{courseId}/lectures/{lectureId}/comments")
    public List<StudentCommentDTO> getComments(
            @PathVariable String studentId,
            @PathVariable String courseId,
            @PathVariable String lectureId,
            @RequestParam(defaultValue = "recent") String sortBy) {
        return commentService.getCommentsThreaded(studentId, lectureId, sortBy);
    }

    @PostMapping("/courses/{courseId}/lectures/{lectureId}/comments")
    public StudentCommentDTO createComment(
            @PathVariable String studentId,
            @PathVariable String courseId,
            @PathVariable String lectureId,
            @RequestBody CommentCreateRequest request) {
        return commentService.createComment(studentId, courseId, lectureId, request);
    }

    @PostMapping("/comments/{commentId}/replies")
    public StudentCommentDTO replyToComment(
            @PathVariable String studentId,
            @PathVariable String commentId,
            @RequestBody CommentCreateRequest request) {
        return commentService.replyToComment(studentId, commentId, request);
    }

    @PostMapping("/comments/{commentId}/like")
    public void toggleLike(
            @PathVariable String studentId,
            @PathVariable String commentId) {
        commentService.toggleLike(studentId, commentId);
    }
}