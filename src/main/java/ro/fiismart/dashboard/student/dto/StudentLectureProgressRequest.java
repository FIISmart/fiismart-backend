package ro.fiismart.dashboard.student.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StudentLectureProgressRequest {
    @Min(0) @Max(100)
    private int watchedPercent;
    @Min(0)
    private int positionSecs;
    private boolean completed;
    private Integer durationSecs;
}
