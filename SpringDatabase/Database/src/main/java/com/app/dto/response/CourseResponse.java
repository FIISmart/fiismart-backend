package com.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private String id;
    private String title;
    private String description;
    private String teacherId;
    private String status;
    private List<String> tags;
    private String thumbnailUrl;
    private String language;
    private int enrollmentCount;
    private double avgRating;
    private List<LectureResponse> lectures;
    private boolean hidden;
    private String quizId;
    private Date createdAt;
    private Date updatedAt;
}
