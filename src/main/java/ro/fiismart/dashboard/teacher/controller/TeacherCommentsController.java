package ro.fiismart.dashboard.teacher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.teacher.dto.TeacherCommentPreviewDTO;
import ro.fiismart.dashboard.teacher.service.TeacherCommentsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSOR')")
public class TeacherCommentsController {

    private final TeacherCommentsService teacherCommentsService;

    @GetMapping("/me/comments")
    public List<TeacherCommentPreviewDTO> getComments(
            @AuthenticationPrincipal String teacherId,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return teacherCommentsService.getComments(teacherId, limit, offset);
    }
}