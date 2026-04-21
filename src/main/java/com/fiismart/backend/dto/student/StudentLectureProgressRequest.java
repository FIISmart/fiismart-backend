package com.fiismart.backend.dto.student;

import lombok.Data;

@Data
public class StudentLectureProgressRequest {
    private int watchedPercent;
    private int positionSecs;
    private boolean completed;
}