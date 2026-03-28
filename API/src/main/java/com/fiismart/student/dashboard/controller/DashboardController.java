package com.fiismart.student.dashboard.controller;

import com.fiismart.student.dashboard.dto.ContinueLearningDTO;
import com.fiismart.student.dashboard.dto.QuizStudentDTO;
import com.fiismart.student.dashboard.dto.StudentAnswerDTO;
import com.fiismart.student.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{studentId}/quizzes")
    public List<QuizStudentDTO> getStudentQuizzes(@PathVariable String studentId) {
        return dashboardService.getQuizzesForStudent(studentId);
    }

    @GetMapping("/{studentId}/continue")
    public ContinueLearningDTO getContinueLearning(@PathVariable String studentId) {
        return dashboardService.getLastAccessedCourse(studentId);
    }

    @GetMapping("/{studentId}/answers")
    public List<StudentAnswerDTO> getStudentAnswers(@PathVariable String studentId) {
        return dashboardService.getAnswersForStudent(studentId);
    }

}

