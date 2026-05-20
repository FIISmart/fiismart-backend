package ro.fiismart.dashboard.teacher.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.fiismart.dashboard.teacher.dto.*;
import ro.fiismart.dashboard.teacher.service.*;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TeacherControllersTest {

    @Mock private TeacherOverviewService teacherOverviewService;
    @Mock private TeacherStatsService teacherStatsService;
    @Mock private TeacherCoursesService teacherCoursesService;
    @Mock private TeacherQuizzesService teacherQuizzesService;
    @Mock private TeacherCommentsService teacherCommentsService;

    private MockMvc overviewMvc;
    private MockMvc statsMvc;
    private MockMvc coursesMvc;
    private MockMvc quizzesMvc;
    private MockMvc commentsMvc;

    @BeforeEach
    void setUp() {
        overviewMvc = MockMvcBuilders.standaloneSetup(
                new TeacherOverviewController(teacherOverviewService)).build();
        statsMvc = MockMvcBuilders.standaloneSetup(
                new TeacherStatsController(teacherStatsService)).build();
        coursesMvc = MockMvcBuilders.standaloneSetup(
                new TeacherCoursesController(teacherCoursesService)).build();
        quizzesMvc = MockMvcBuilders.standaloneSetup(
                new TeacherQuizController(teacherQuizzesService)).build();
        commentsMvc = MockMvcBuilders.standaloneSetup(
                new TeacherCommentsController(teacherCommentsService)).build();
    }

    @Test
    void getOverview_returnsOk() throws Exception {
        when(teacherOverviewService.getOverview(any(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new TeacherOverviewDTO());

        overviewMvc.perform(get("/api/v1/teacher-dashboard/me/overview"))
                .andExpect(status().isOk());
    }

    @Test
    void getStats_returnsOk() throws Exception {
        when(teacherStatsService.getStats(any())).thenReturn(new TeacherStatsDTO());

        statsMvc.perform(get("/api/v1/teacher-dashboard/me/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void getCourses_returnsOk() throws Exception {
        when(teacherCoursesService.getCourses(any(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());

        coursesMvc.perform(get("/api/v1/teacher-dashboard/me/courses"))
                .andExpect(status().isOk());
    }

    @Test
    void getQuizzes_returnsOk() throws Exception {
        when(teacherQuizzesService.getQuizzes(any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        quizzesMvc.perform(get("/api/v1/teacher-dashboard/me/quizzes"))
                .andExpect(status().isOk());
    }

    @Test
    void getComments_returnsOk() throws Exception {
        when(teacherCommentsService.getComments(any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        commentsMvc.perform(get("/api/v1/teacher-dashboard/me/comments"))
                .andExpect(status().isOk());
    }
}
