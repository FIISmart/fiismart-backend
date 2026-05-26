package ro.fiismart.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalStudents;
    private long totalProfessors;
    private long totalAdmins;
    private long totalCourses;
    private long publishedCourses;
    private long totalEnrollments;
    private long totalComments;
}
