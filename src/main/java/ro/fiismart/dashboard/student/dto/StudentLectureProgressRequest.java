package ro.fiismart.dashboard.student.dto;

import lombok.Data;

@Data
public class StudentLectureProgressRequest {
    private int watchedPercent;
    private int positionSecs;
    private boolean completed;
    private Integer durationSecs;
}
