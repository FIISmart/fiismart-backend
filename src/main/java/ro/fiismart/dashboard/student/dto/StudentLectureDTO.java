package ro.fiismart.dashboard.student.dto;

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
