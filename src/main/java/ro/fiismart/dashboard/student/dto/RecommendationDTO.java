package ro.fiismart.dashboard.student.dto;

import lombok.Data;

@Data
public class RecommendationDTO {
    private String courseId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private double avgRating;
    private int enrollmentCount;
}
