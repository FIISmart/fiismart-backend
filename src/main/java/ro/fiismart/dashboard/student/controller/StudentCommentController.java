package ro.fiismart.dashboard.student.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.util.AuthUtils;
import ro.fiismart.dashboard.student.dto.CommentCreateRequest;
import ro.fiismart.dashboard.student.dto.StudentCommentDTO;
import ro.fiismart.dashboard.student.service.StudentCommentService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/{studentId}")
@PreAuthorize("hasRole('STUDENT')")
public class StudentCommentController {

    private final StudentCommentService commentService;

    public StudentCommentController(StudentCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/courses/{courseId}/lectures/{lectureId}/comments")
    public List<StudentCommentDTO> getComments(@PathVariable String studentId,
                                                @PathVariable String courseId,
                                                @PathVariable String lectureId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own data");
        }
        return commentService.getCommentsThreaded(studentId, lectureId);
    }

    @PostMapping("/courses/{courseId}/lectures/{lectureId}/comments")
public StudentCommentDTO createComment(@PathVariable String studentId,
                                             @PathVariable String courseId,
                                             @PathVariable String lectureId,
                                             @Valid @RequestBody CommentCreateRequest request) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only comment on your own behalf");
        }
        return commentService.createComment(studentId, courseId, lectureId, request);
    }

    @PostMapping("/comments/{commentId}/replies")
public StudentCommentDTO replyToComment(@PathVariable String studentId,
                                              @PathVariable String commentId,
                                              @Valid @RequestBody CommentCreateRequest request) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only reply on your own behalf");
        }
        return commentService.replyToComment(studentId, commentId, request);
    }

    @PostMapping("/comments/{commentId}/like")
    public void toggleLike(@PathVariable String studentId, @PathVariable String commentId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only like on your own behalf");
        }
        commentService.toggleLike(studentId, commentId);
    }
}