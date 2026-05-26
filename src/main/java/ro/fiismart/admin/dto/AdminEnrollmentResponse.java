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
    private String studentName;
    private String courseId;
    private String courseTitle;
    private String status;
    private int overallProgress;
    private Date enrolledAt;

    public static AdminEnrollmentResponse from(Enrollment e, String studentName, String courseTitle) {
        return AdminEnrollmentResponse.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .studentName(studentName)
                .courseId(e.getCourseId())
                .courseTitle(courseTitle)
                .status(e.getStatus())
                .overallProgress(e.getOverallProgress())
                .enrolledAt(e.getEnrolledAt())
                .build();
    }
}
