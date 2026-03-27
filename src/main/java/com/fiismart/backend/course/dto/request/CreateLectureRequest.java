package com.fiismart.backend.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateLectureRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String videoUrl;
    private List<String> imageUrls;
    private int order;
    private int durationSecs;
}
