package ro.fiismart.courses.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UpdateLectureRequest {
    private String title;
    private String type;
    private String content;
    private String videoUrl;
    private String pdfUrl;
    private List<String> imageUrls;
    private int durationSecs;
}
