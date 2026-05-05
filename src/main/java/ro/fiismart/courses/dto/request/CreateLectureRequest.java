package ro.fiismart.courses.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CreateLectureRequest {

    @NotBlank(message = "Lecture title is required")
    private String title;

    private String videoUrl;
    private List<String> imageUrls;
    private int durationSecs;
}
