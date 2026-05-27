package ro.fiismart.dashboard.teacher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.teacher.dto.TeacherCommentPreviewDTO;
import ro.fiismart.dashboard.teacher.service.TeacherCommentsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher-dashboard")
@RequiredArgsConstructor
public class TeacherCommentsController {

    private final TeacherCommentsService teacherCommentsService;

    @GetMapping("/me/comments")
    public List<TeacherCommentPreviewDTO> getComments(
            @AuthenticationPrincipal String teacherId,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return teacherCommentsService.getComments(teacherId, limit, offset);
    }

    @PostMapping("/comments/{commentId}/replies")
    public TeacherCommentPreviewDTO reply(
            @AuthenticationPrincipal String teacherId,
            @PathVariable String commentId,
            @RequestBody Map<String, String> body) {
        return teacherCommentsService.reply(teacherId, commentId, body.get("body"));
    }

    @PatchMapping("/comments/{commentId}/status")
    public TeacherCommentPreviewDTO updateStatus(
            @AuthenticationPrincipal String teacherId,
            @PathVariable String commentId,
            @RequestBody Map<String, String> body) {
        return teacherCommentsService.updateStatus(teacherId, commentId, body.get("status"));
    }
}
