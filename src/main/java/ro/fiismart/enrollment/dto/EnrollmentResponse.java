package ro.fiismart.enrollment.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.LectureProgressEntry;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class EnrollmentResponse {

    private String id;
    private String studentId;
    private String courseId;
    private Date enrolledAt;
    private Date completedAt;
    private String status;
    private List<LectureProgressEntry> lectureProgress;
    private Date lastAccessedAt;
    private double overallProgress;
}
