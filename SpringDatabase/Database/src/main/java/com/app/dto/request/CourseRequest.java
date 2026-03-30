package com.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String teacherId;

    private String status;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private String thumbnailUrl;
    private String language;
}
