package ro.fiismart.dashboard.teacher.dto;

import lombok.Data;
import java.util.Date;

@Data
public class TeacherCoursesDTO {
    private String courseId;
    private String title;
    private String description;
    private String status;
    private int enrollmentCount;
    private double avgRating;
    private Date updatedAt;
    private String thumbnailUrl;
}
