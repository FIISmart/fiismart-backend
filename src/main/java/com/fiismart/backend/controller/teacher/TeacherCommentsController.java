package com.fiismart.backend.controller.teacher;

import com.fiismart.backend.dto.teacher.TeacherCommentPreviewDTO;
import com.fiismart.backend.service.teacher.TeacherCommentsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher-dashboard")
@CrossOrigin(origins = "*")
public class TeacherCommentsController {

    private final TeacherCommentsService teacherCommentsService;

    public TeacherCommentsController(TeacherCommentsService teacherCommentsService) {
        this.teacherCommentsService = teacherCommentsService;
    }

    @GetMapping("/me/comments")
    public List<TeacherCommentPreviewDTO> getComments(
            @RequestHeader("X-Dev-UserId") String teacherId,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return teacherCommentsService.getComments(teacherId, limit, offset);
    }
}

