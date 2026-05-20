package ro.fiismart.dashboard.teacher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.dashboard.teacher.dto.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherOverviewServiceTest {

    @Mock
    private TeacherStatsService teacherStatsService;
    @Mock
    private TeacherCoursesService teacherCoursesService;
    @Mock
    private TeacherQuizzesService teacherQuizzesService;
    @Mock
    private TeacherCommentsService teacherCommentsService;

    @InjectMocks
    private TeacherOverviewService teacherOverviewService;

    @Test
    void getOverview_aggregatesAllServices() {
        TeacherStatsDTO stats = new TeacherStatsDTO();
        stats.setActiveCourses(3);

        when(teacherStatsService.getStats("teacher1")).thenReturn(stats);
        when(teacherCoursesService.getCourses("teacher1", "all", 5, 0)).thenReturn(List.of());
        when(teacherQuizzesService.getQuizzes("teacher1", 3, 0)).thenReturn(List.of());
        when(teacherCommentsService.getComments("teacher1", 5, 0)).thenReturn(List.of());

        TeacherOverviewDTO result = teacherOverviewService.getOverview("teacher1", 5, 3, 5);

        assertThat(result.getStats()).isEqualTo(stats);
        assertThat(result.getStats().getActiveCourses()).isEqualTo(3);
        assertThat(result.getCoursesPreview()).isEmpty();
        assertThat(result.getQuizzesPreview()).isEmpty();
        assertThat(result.getCommentsPreview()).isEmpty();
    }

    @Test
    void getOverview_callsAllServicesWithCorrectLimits() {
        when(teacherStatsService.getStats("teacher1")).thenReturn(new TeacherStatsDTO());
        when(teacherCoursesService.getCourses(any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        when(teacherQuizzesService.getQuizzes(any(), anyInt(), anyInt())).thenReturn(List.of());
        when(teacherCommentsService.getComments(any(), anyInt(), anyInt())).thenReturn(List.of());

        teacherOverviewService.getOverview("teacher1", 10, 5, 8);

        verify(teacherCoursesService).getCourses("teacher1", "all", 10, 0);
        verify(teacherQuizzesService).getQuizzes("teacher1", 5, 0);
        verify(teacherCommentsService).getComments("teacher1", 8, 0);
    }
}
