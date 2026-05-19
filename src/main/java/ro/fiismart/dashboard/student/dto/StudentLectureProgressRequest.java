package ro.fiismart.dashboard.student.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StudentLectureProgressRequest {

    @Min(value = 0, message = "watchedPercent must be >= 0")
    @Max(value = 100, message = "watchedPercent must be <= 100")
    private int watchedPercent;

    @Min(value = 0, message = "positionSecs must be >= 0")
    private int positionSecs;

    private boolean completed;

    /**
     * @deprecated ignored server-side. Lecture duration is instructor-owned
     * metadata; allowing student-authenticated writes to mutate the canonical
     * Course definition was an authorization regression.
     */
    @Deprecated
    private Integer durationSecs;
}
