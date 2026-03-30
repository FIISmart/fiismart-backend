package com.fiismart.teacher_dashboard.service;

import com.fiismart.teacher_dashboard.dto.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TeacherOverviewService {

    private final TeacherStatsService teacherStatsService;
    private final TeacherCoursesService teacherCoursesService;
    private final TeacherQuizzesService teacherQuizzesService;
    private final TeacherCommentsService teacherCommentsService;

    public TeacherOverviewService(
            TeacherStatsService teacherStatsService,
            TeacherCoursesService teacherCoursesService,
            TeacherQuizzesService teacherQuizzesService,
            TeacherCommentsService teacherCommentsService) {
        this.teacherStatsService = teacherStatsService;
        this.teacherCoursesService = teacherCoursesService;
        this.teacherQuizzesService = teacherQuizzesService;
        this.teacherCommentsService = teacherCommentsService;
    }

    public TeacherOverviewDTO getOverview(
            String teacherIdHex,
            int coursesLimit,
            int quizzesLimit,
            int commentsLimit) {

        TeacherStatsDTO stats = teacherStatsService.getStats(teacherIdHex);
        List<TeacherCoursesDTO> coursesPreview = teacherCoursesService.getCourses(teacherIdHex, "all", coursesLimit, 0);
        List<TeacherQuizPreviewDTO> quizzesPreview = teacherQuizzesService.getQuizzes(teacherIdHex, quizzesLimit, 0);
        List<TeacherCommentPreviewDTO> commentsPreview = teacherCommentsService.getComments(teacherIdHex, commentsLimit, 0);

        TeacherOverviewDTO dto = new TeacherOverviewDTO();
        dto.setStats(stats);
        dto.setCoursesPreview(coursesPreview);
        dto.setQuizzesPreview(quizzesPreview);
        dto.setCommentsPreview(commentsPreview);

        return dto;
    }
}
