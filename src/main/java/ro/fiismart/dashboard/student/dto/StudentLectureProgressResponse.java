package ro.fiismart.dashboard.student.dto;

import lombok.Data;

@Data
public class StudentLectureProgressResponse {
    private String lectureId;
    private int watchedPercent;
    private int positionSecs;
    private boolean completed;
    private int overallProgress;
    private String enrollmentStatus;
    private boolean courseCompleted;
}
