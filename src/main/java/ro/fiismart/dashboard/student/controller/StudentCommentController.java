package ro.fiismart.dashboard.student.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public List<StudentCommentDTO> getComments(@AuthenticationPrincipal String authenticatedStudentId,
                                                @PathVariable String studentId,
                                                @PathVariable String courseId,
                                                @PathVariable String lectureId) {
        validateStudent(authenticatedStudentId, studentId);
        return commentService.getCommentsThreaded(studentId, lectureId);
    }

    @PostMapping("/courses/{courseId}/lectures/{lectureId}/comments")
    public StudentCommentDTO createComment(@AuthenticationPrincipal String authenticatedStudentId,
                                            @PathVariable String studentId,
                                            @PathVariable String courseId,
                                            @PathVariable String lectureId,
                                            @RequestBody CommentCreateRequest request) {
        validateStudent(authenticatedStudentId, studentId);
        return commentService.createComment(studentId, courseId, lectureId, request);
    }

    @PostMapping("/comments/{commentId}/replies")
    public StudentCommentDTO replyToComment(@AuthenticationPrincipal String authenticatedStudentId,
                                             @PathVariable String studentId,
                                             @PathVariable String commentId,
                                             @RequestBody CommentCreateRequest request) {
        validateStudent(authenticatedStudentId, studentId);
        return commentService.replyToComment(studentId, commentId, request);
    }

    @PostMapping("/comments/{commentId}/like")
    public void toggleLike(@AuthenticationPrincipal String authenticatedStudentId,
                            @PathVariable String studentId,
                            @PathVariable String commentId) {
        validateStudent(authenticatedStudentId, studentId);
        commentService.toggleLike(studentId, commentId);
    }

    private void validateStudent(String authenticatedStudentId, String studentId) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
