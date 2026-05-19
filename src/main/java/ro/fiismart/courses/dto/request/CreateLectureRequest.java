package ro.fiismart.courses.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CreateLectureRequest {

    @NotBlank(message = "Lecture title is required")
    private String title;

    private String type;
    private String content;
    private String videoUrl;
    private String pdfUrl;
    private List<String> imageUrls;
    private int durationSecs;
}
