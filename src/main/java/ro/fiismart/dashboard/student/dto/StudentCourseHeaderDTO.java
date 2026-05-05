package ro.fiismart.dashboard.student.dto;

import lombok.Data;
import java.util.List;

@Data
public class StudentCourseHeaderDTO {
    private String courseId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String language;
    private String status;
    private List<String> tags;
    private String teacherId;
    private String teacherDisplayName;
    private double avgRating;
    private int enrollmentCount;
    private boolean enrolled;
    private int overallProgress;
}
