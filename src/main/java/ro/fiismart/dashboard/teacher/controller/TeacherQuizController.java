package ro.fiismart.dashboard.teacher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.teacher.dto.TeacherQuizPreviewDTO;
import ro.fiismart.dashboard.teacher.service.TeacherQuizzesService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-dashboard")
@RequiredArgsConstructor
public class TeacherQuizController {

    private final TeacherQuizzesService teacherQuizzesService;

    @GetMapping("/me/quizzes")
    public List<TeacherQuizPreviewDTO> getQuizzes(
            @AuthenticationPrincipal String teacherId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return teacherQuizzesService.getQuizzes(teacherId, limit, offset);
    }
}
