package ro.fiismart.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureProgressEntry {

    @NotBlank
    private String lectureId;
    private String moduleId;
    @Min(0) @Max(100)
    private int watchedPercent;
    @Min(0)
    private int positionSecs;
    private boolean completed;
    private Date updatedAt;
}
