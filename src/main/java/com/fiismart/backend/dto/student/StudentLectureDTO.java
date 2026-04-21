package com.fiismart.backend.dto.student;

import lombok.Data;

@Data
public class StudentLectureDTO {
    private String lectureId;
    private String title;
    private int order;
    private int durationSecs;

    private int watchedPercent;
    private int positionSecs;
    private boolean completed;
}