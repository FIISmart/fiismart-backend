package ro.fiismart.users.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PublicUserProfileResponse {
    private String id;
    private String displayName;
    private String role;
    private String avatarUrl;
    private String headline;
    private String bio;
    private String faculty;
    private String specialization;
    private Integer studyYear;
    private String educationLevel;
    private String department;
    private String academicTitle;
    private List<String> interests;
    private List<String> subjects;
    private Boolean tutorProfileEnabled;
    private Double tutorRating;
    private Integer tutorReviewCount;
    private Integer experienceYears;
    private String availability;
    private String priceLabel;
    private long publishedCourseCount;
}
