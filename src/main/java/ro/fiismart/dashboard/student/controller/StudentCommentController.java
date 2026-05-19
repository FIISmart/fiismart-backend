package ro.fiismart.dashboard.student.controller;

import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.student.dto.CommentCreateRequest;
import ro.fiismart.dashboard.student.dto.StudentCommentDTO;
import ro.fiismart.dashboard.student.service.StudentCommentService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/{studentId}")
public class StudentCommentController {

    private final StudentCommentService commentService;

    public StudentCommentController(StudentCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/courses/{courseId}/lectures/{lectureId}/comments")
    public List<StudentCommentDTO> getComments(@PathVariable String studentId,
                                                @PathVariable String courseId,
                                                @PathVariable String lectureId) {
        return commentService.getCommentsThreaded(studentId, lectureId);
    }

    @PostMapping("/courses/{courseId}/lectures/{lectureId}/comments")
    public StudentCommentDTO createComment(@PathVariable String studentId,
                                            @PathVariable String courseId,
                                            @PathVariable String lectureId,
                                            @RequestBody CommentCreateRequest request) {
        return commentService.createComment(studentId, courseId, lectureId, request);
    }

    @PostMapping("/comments/{commentId}/replies")
    public StudentCommentDTO replyToComment(@PathVariable String studentId,
                                             @PathVariable String commentId,
                                             @RequestBody CommentCreateRequest request) {
        return commentService.replyToComment(studentId, commentId, request);
    }

    @PostMapping("/comments/{commentId}/like")
    public void toggleLike(@PathVariable String studentId, @PathVariable String commentId) {
        commentService.toggleLike(studentId, commentId);
    }
}
