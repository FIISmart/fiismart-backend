package com.fiismart.backend.dto.student;

import lombok.Data;

@Data
public class StudentLectureProgressResponse {
    // Progresul lecturii (ce am salvat)
    private String lectureId;
    private int watchedPercent;
    private int positionSecs;
    private boolean completed;

    // Progresul agregat al cursului (recalculat)
    private int overallProgress;
    private String enrollmentStatus;
    private boolean courseCompleted;
}