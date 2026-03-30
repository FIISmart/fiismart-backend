package com.fiismart.teacher_dashboard.dto;

import lombok.Data;
import java.util.Date;

@Data
public class TeacherCoursesDTO {
    private String courseId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String status;
    private int enrollmentCount;
    private double avgRating;
    private Date updatedAt;

}
