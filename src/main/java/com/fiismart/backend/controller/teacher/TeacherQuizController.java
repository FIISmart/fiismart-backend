package com.fiismart.backend.controller.teacher;

import com.fiismart.backend.dto.teacher.TeacherQuizPreviewDTO;
import com.fiismart.backend.service.teacher.TeacherQuizzesService;
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
