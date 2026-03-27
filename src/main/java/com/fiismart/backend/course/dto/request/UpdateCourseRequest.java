package com.fiismart.backend.course.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateCourseRequest {

    private String title;
    private String description;
    private List<String> tags;
    private String thumbnailUrl;
    private String language;
}
