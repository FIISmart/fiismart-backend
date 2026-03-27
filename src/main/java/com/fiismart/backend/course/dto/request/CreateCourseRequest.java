package com.fiismart.backend.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateCourseRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Teacher ID is required")
    private String teacherId;

    private List<String> tags;
    private String thumbnailUrl;
    private String language;
}
