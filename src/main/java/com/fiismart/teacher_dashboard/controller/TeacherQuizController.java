package com.fiismart.teacher_dashboard.controller;

import com.fiismart.teacher_dashboard.dto.TeacherQuizPreviewDTO;
import com.fiismart.teacher_dashboard.service.TeacherQuizzesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher-dashboard")
@CrossOrigin(origins = "*")

public class TeacherQuizController {
    private final TeacherQuizzesService teacherQuizzesService;

    public TeacherQuizController(TeacherQuizzesService teacherQuizzesService){
        this.teacherQuizzesService = teacherQuizzesService;
    }
    @GetMapping("/me/quizzes")
    public List<TeacherQuizPreviewDTO> getQuizzes(
            @RequestHeader("X-Dev-UserId") String teacherId,
            @RequestParam(defaultValue = "3") int limit,
            @RequestParam(defaultValue = "0") int offset
    ){
        return teacherQuizzesService.getQuizzes(teacherId,limit,offset);
    }
}
