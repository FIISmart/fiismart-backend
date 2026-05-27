package ro.fiismart.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileRequest {
    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String lastName;

    @Size(max = 160)
    private String displayName;

    @Size(max = 40)
    private String phone;

    @Size(max = 1000)
    private String bio;

    @Size(max = 500)
    private String avatarUrl;

    @Size(max = 120)
    private String faculty;

    @Size(max = 120)
    private String specialization;

    private Integer studyYear;

    @Size(max = 40)
    private String educationLevel;

    @Size(max = 120)
    private String department;

    @Size(max = 120)
    private String academicTitle;

    @Size(max = 20)
    private List<@Size(max = 60) String> interests;

    @Size(max = 20)
    private List<@Size(max = 60) String> subjects;

    private Boolean tutorProfileEnabled;
}
