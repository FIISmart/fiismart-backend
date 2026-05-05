package ro.fiismart.courses.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UpdateLectureRequest {
    private String title;
    private String videoUrl;
    private List<String> imageUrls;
    private int durationSecs;
}
