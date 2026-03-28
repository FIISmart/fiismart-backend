package com.fiismart.student.dashboard.dto;

import lombok.Data;

@Data
public class CourseSummaryDTO {
    private String courseId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private double avgRating;
    private int enrollmentCount;
    private int overallProgress;
    private String status;
}
