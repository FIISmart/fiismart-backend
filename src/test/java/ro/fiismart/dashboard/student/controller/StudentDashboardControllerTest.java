package ro.fiismart.dashboard.student.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.fiismart.dashboard.student.dto.*;
import ro.fiismart.dashboard.student.service.*;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudentDashboardControllerTest {

    @Mock private StudentDashboardService dashboardService;
    @Mock private StudentStatsService statsService;
    @Mock private StudentCoursesService coursesService;
    @Mock private StudentInitialsService initialsService;
    @Mock private StudentRecommendationService recommendationService;

    @InjectMocks private StudentDashboardController studentDashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studentDashboardController).build();
    }

    @Test
    void getStats_returnsOk() throws Exception {
        StatsDTO stats = new StatsDTO();
        when(statsService.getStats("s1")).thenReturn(stats);

        mockMvc.perform(get("/api/v1/student-dashboard/s1/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void getCourses_returnsOk() throws Exception {
        when(coursesService.getCourses("s1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/student-dashboard/s1/courses"))
                .andExpect(status().isOk());
    }

    @Test
    void getInitials_returnsOk() throws Exception {
        InitialsDTO dto = new InitialsDTO();
        when(initialsService.getInitials("s1")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/student-dashboard/s1/initials"))
                .andExpect(status().isOk());
    }

    @Test
    void getRecommendation_returnsOk() throws Exception {
        when(recommendationService.getRecommendation("s1")).thenReturn(null);

        mockMvc.perform(get("/api/v1/student-dashboard/s1/recommendations"))
                .andExpect(status().isOk());
    }

    @Test
    void getQuizzes_returnsOk() throws Exception {
        when(dashboardService.getQuizzesForStudent("s1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/student-dashboard/s1/quizzes"))
                .andExpect(status().isOk());
    }

    @Test
    void getContinueLearning_returnsOk() throws Exception {
        when(dashboardService.getLastAccessedCourse("s1")).thenReturn(null);

        mockMvc.perform(get("/api/v1/student-dashboard/s1/continue"))
                .andExpect(status().isOk());
    }

    @Test
    void getAnswers_returnsOk() throws Exception {
        when(dashboardService.getAnswersForStudent("s1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/student-dashboard/s1/answers"))
                .andExpect(status().isOk());
    }

    @Test
    void getName_returnsOk() throws Exception {
        UserNameDTO dto = new UserNameDTO();
        when(dashboardService.getStudentName("s1")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/student-dashboard/s1/name"))
                .andExpect(status().isOk());
    }
}
