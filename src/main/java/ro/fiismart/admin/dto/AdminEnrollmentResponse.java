package ro.fiismart.admin.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.Enrollment;

import java.util.Date;

@Data
@Builder
public class AdminEnrollmentResponse {
    private String id;
    private String studentId;
    private String courseId;
    private String status;
    private int overallProgress;
    private Date enrolledAt;

    public static AdminEnrollmentResponse from(Enrollment e) {
        return AdminEnrollmentResponse.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .courseId(e.getCourseId())
                .status(e.getStatus())
                .overallProgress(e.getOverallProgress())
                .enrolledAt(e.getEnrolledAt())
                .build();
    }
}
