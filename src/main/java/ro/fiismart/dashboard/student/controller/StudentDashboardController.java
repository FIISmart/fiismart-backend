package ro.fiismart.dashboard.student.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public StatsDTO getStats(@AuthenticationPrincipal String authenticatedStudentId, @PathVariable String studentId) {
        validateStudent(authenticatedStudentId, studentId);
        return statsService.getStats(studentId);
    }

    @GetMapping("/{studentId}/courses")
    public List<CourseSummaryDTO> getCourses(@AuthenticationPrincipal String authenticatedStudentId, @PathVariable String studentId) {
        validateStudent(authenticatedStudentId, studentId);
        return coursesService.getCourses(studentId);
    }

    @GetMapping("/{studentId}/initials")
    public InitialsDTO getInitials(@AuthenticationPrincipal String authenticatedStudentId, @PathVariable String studentId) {
        validateStudent(authenticatedStudentId, studentId);
        return initialsService.getInitials(studentId);
    }

    @GetMapping("/{studentId}/recommendations")
    public RecommendationDTO getRecommendation(@AuthenticationPrincipal String authenticatedStudentId, @PathVariable String studentId) {
        validateStudent(authenticatedStudentId, studentId);
        return recommendationService.getRecommendation(studentId);
    }

    @GetMapping("/{studentId}/quizzes")
    public List<QuizStudentDTO> getQuizzes(@AuthenticationPrincipal String authenticatedStudentId, @PathVariable String studentId) {
        validateStudent(authenticatedStudentId, studentId);
        return dashboardService.getQuizzesForStudent(studentId);
    }

    @GetMapping("/{studentId}/continue")
    public ContinueLearningDTO getContinueLearning(@AuthenticationPrincipal String authenticatedStudentId, @PathVariable String studentId) {
        validateStudent(authenticatedStudentId, studentId);
        return dashboardService.getLastAccessedCourse(studentId);
    }

    @GetMapping("/{studentId}/answers")
    public List<StudentAnswerDTO> getAnswers(@AuthenticationPrincipal String authenticatedStudentId, @PathVariable String studentId) {
        validateStudent(authenticatedStudentId, studentId);
        return dashboardService.getAnswersForStudent(studentId);
    }

    @GetMapping("/{studentId}/name")
    public UserNameDTO getName(@AuthenticationPrincipal String authenticatedStudentId, @PathVariable String studentId) {
        validateStudent(authenticatedStudentId, studentId);
        return dashboardService.getStudentName(studentId);
    }

    private void validateStudent(String authenticatedStudentId, String studentId) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
