package ro.fiismart.dashboard.student.dto;

import lombok.Data;

@Data
public class StudentLectureDTO {
    private String lectureId;
    private String title;
    private String type;
    private String content;
    private String videoUrl;
    private String pdfUrl;
    private int order;
    private int durationSecs;
    private int watchedPercent;
    private int positionSecs;
    private boolean completed;
    private StudentQuizStatusDTO quiz;
}
