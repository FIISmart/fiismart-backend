package ro.fiismart.tutors.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TutorResponse {
    private String id;
    private String displayName;
    private String headline;
    private List<String> tags;
    private String bio;
    private int courseCount;
    private int publishedCourseCount;
    private double avgRating;
    private int reviewCount;
    private int experienceYears;
    private String avatarUrl;
    private String availability;
    private String priceLabel;
}
