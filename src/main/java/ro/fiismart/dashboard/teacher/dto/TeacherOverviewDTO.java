package ro.fiismart.dashboard.teacher.dto;

import lombok.Data;
import java.util.List;

@Data
public class TeacherOverviewDTO {
    private TeacherStatsDTO stats;
    private List<TeacherCoursesDTO> coursesPreview;
    private List<TeacherQuizPreviewDTO> quizzesPreview;
    private List<TeacherCommentPreviewDTO> commentsPreview;
}
