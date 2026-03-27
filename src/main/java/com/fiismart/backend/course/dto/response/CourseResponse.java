package com.fiismart.backend.course.dto.response;

import database.model.Course;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
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
    private boolean isHidden;
    private String quizId;
    private Date createdAt;
    private Date updatedAt;
    private List<LectureResponse> lectures;

    public static CourseResponse fromModel(Course course) {
        if (course == null) return null;
        return CourseResponse.builder()
                .id(course.getId() != null ? course.getId().toHexString() : null)
                .title(course.getTitle())
                .description(course.getDescription())
                .teacherId(course.getTeacherId() != null ? course.getTeacherId().toHexString() : null)
                .status(course.getStatus())
                .tags(course.getTags())
                .thumbnailUrl(course.getThumbnailUrl())
                .language(course.getLanguage())
                .enrollmentCount(course.getEnrollmentCount())
                .avgRating(course.getAvgRating())
                .isHidden(course.isHidden())
                .quizId(course.getQuizId() != null ? course.getQuizId().toHexString() : null)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .lectures(course.getLectures() != null
                        ? course.getLectures().stream().map(LectureResponse::fromModel).collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
