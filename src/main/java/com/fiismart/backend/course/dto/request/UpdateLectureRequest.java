package com.fiismart.backend.course.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateLectureRequest {

    private String title;
    private String videoUrl;
    private List<String> imageUrls;
    private Integer order;
    private Integer durationSecs;
}
