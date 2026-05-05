package ro.fiismart.dashboard.teacher.service;

import org.springframework.stereotype.Service;
import ro.fiismart.dashboard.teacher.dto.*;

import java.util.List;

@Service
public class TeacherOverviewService {

    private final TeacherStatsService teacherStatsService;
    private final TeacherCoursesService teacherCoursesService;
    private final TeacherQuizzesService teacherQuizzesService;
    private final TeacherCommentsService teacherCommentsService;

    public TeacherOverviewService(TeacherStatsService teacherStatsService,
                                  TeacherCoursesService teacherCoursesService,
                                  TeacherQuizzesService teacherQuizzesService,
                                  TeacherCommentsService teacherCommentsService) {
        this.teacherStatsService = teacherStatsService;
        this.teacherCoursesService = teacherCoursesService;
        this.teacherQuizzesService = teacherQuizzesService;
        this.teacherCommentsService = teacherCommentsService;
    }

    public TeacherOverviewDTO getOverview(String teacherId, int coursesLimit,
                                          int quizzesLimit, int commentsLimit) {
        TeacherOverviewDTO dto = new TeacherOverviewDTO();
        dto.setStats(teacherStatsService.getStats(teacherId));
        dto.setCoursesPreview(teacherCoursesService.getCourses(teacherId, "all", coursesLimit, 0));
        dto.setQuizzesPreview(teacherQuizzesService.getQuizzes(teacherId, quizzesLimit, 0));
        dto.setCommentsPreview(teacherCommentsService.getComments(teacherId, commentsLimit, 0));
        return dto;
    }
}
