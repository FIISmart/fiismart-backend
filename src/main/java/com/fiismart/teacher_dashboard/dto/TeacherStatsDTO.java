package com.fiismart.teacher_dashboard.dto;

import lombok.Data;

@Data
public class TeacherStatsDTO {
    private int studentsEnrolled;
    private int activeCourses;
    private int quizzesCompleted;
    private double completionRatePct;
}
