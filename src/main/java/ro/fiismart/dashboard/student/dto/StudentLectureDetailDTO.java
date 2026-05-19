package ro.fiismart.dashboard.student.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class StudentLectureDetailDTO {
    private String lectureId;
    private String title;
    private String type;
    private String content;
    private String videoUrl;
    private String pdfUrl;
    private List<String> imageUrls;
    private int order;
    private int durationSecs;
    private Date publishedAt;
    private int watchedPercent;
    private int positionSecs;
    private boolean completed;
    private StudentQuizStatusDTO quiz;
}
