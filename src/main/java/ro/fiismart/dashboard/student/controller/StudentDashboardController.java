package ro.fiismart.dashboard.student.controller;

import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.student.dto.*;
import ro.fiismart.dashboard.student.service.*;

import java.util.List;

/**
 * Controller principal pentru dashboard-ul studentului:
 * statistici, continuare curs, quizuri, răspunsuri, inițiale, recomandări.
 */
@RestController
@RequestMapping("/api/v1/student-dashboard")
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;
    private final StudentStatsService statsService;
    private final StudentCoursesService coursesService;
    private final StudentInitialsService initialsService;
    private final StudentRecommendationService recommendationService;

    public StudentDashboardController(StudentDashboardService dashboardService,
                                      StudentStatsService statsService,
                                      StudentCoursesService coursesService,
                                      StudentInitialsService initialsService,
                                      StudentRecommendationService recommendationService) {
        this.dashboardService = dashboardService;
        this.statsService = statsService;
        this.coursesService = coursesService;
        this.initialsService = initialsService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{studentId}/stats")
    public StatsDTO getStats(@PathVariable String studentId) {
        return statsService.getStats(studentId);
    }

    @GetMapping("/{studentId}/courses")
    public List<CourseSummaryDTO> getCourses(@PathVariable String studentId) {
        return coursesService.getCourses(studentId);
    }

    @GetMapping("/{studentId}/initials")
    public InitialsDTO getInitials(@PathVariable String studentId) {
        return initialsService.getInitials(studentId);
    }

    @GetMapping("/{studentId}/recommendations")
    public RecommendationDTO getRecommendation(@PathVariable String studentId) {
        return recommendationService.getRecommendation(studentId);
    }

    @GetMapping("/{studentId}/quizzes")
    public List<QuizStudentDTO> getQuizzes(@PathVariable String studentId) {
        return dashboardService.getQuizzesForStudent(studentId);
    }

    @GetMapping("/{studentId}/continue")
    public ContinueLearningDTO getContinueLearning(@PathVariable String studentId) {
        return dashboardService.getLastAccessedCourse(studentId);
    }

    @GetMapping("/{studentId}/answers")
    public List<StudentAnswerDTO> getAnswers(@PathVariable String studentId) {
        return dashboardService.getAnswersForStudent(studentId);
    }

    @GetMapping("/{studentId}/name")
    public UserNameDTO getName(@PathVariable String studentId) {
        return dashboardService.getStudentName(studentId);
    }
}
