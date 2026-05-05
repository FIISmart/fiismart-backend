package ro.fiismart.common.model;

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

    private String lectureId;
    private String moduleId;
    private int watchedPercent;
    private int positionSecs;
    private boolean completed;
    private Date updatedAt;
}
