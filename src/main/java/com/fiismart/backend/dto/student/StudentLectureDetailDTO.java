package com.fiismart.backend.dto.student;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class StudentLectureDetailDTO {
    private String lectureId;
    private String title;
    private String videoUrl;
    private List<String> imageUrls;
    private int order;
    private int durationSecs;
    private Date publishedAt;

    private int watchedPercent;
    private int positionSecs;
    private boolean completed;
}