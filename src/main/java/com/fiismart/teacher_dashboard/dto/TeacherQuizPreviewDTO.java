package com.fiismart.teacher_dashboard.dto;

import lombok.Data;

@Data
public class TeacherQuizPreviewDTO {
    private String quizId;
    private String title;
    private String courseId;
    private String courseTitle;
    private int attemptsCount;
    private double avgScorePct;
    private String status;
}
