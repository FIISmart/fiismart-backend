package com.fiismart.controller;

import com.fiismart.dto.ContinueLearningDTO;
import com.fiismart.dto.QuizStudentDTO;
import com.fiismart.dto.StudentAnswerDTO;
import com.fiismart.service.DashboardService;
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

    @GetMapping("/progress/{studentId}")
    public ResponseEntity<List<CourseProgressDTO>> getDashboardProgress(@PathVariable Long studentId) {
        List<Course> enrolledCourses = courseService.getEnrolledCourses(studentId);

        List<CourseProgressDTO> progressList = enrolledCourses.stream()
                .map(course -> {
                    int progress = courseService.calculateOverallProgress(course.getId(), studentId);
                    return new CourseProgressDTO(course.getName(), progress);
                })
                .toList();

        return ResponseEntity.ok(progressList);
    }
}
