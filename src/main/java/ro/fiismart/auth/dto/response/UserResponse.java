package ro.fiismart.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String cognitoSub;
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String role;          // "STUDENT" sau "PROFESSOR" (uppercase, compatibil cu frontul)
    private String phone;
    private String bio;
    private String avatarUrl;
    private String faculty;
    private String specialization;
    private Integer studyYear;
    private String educationLevel;
    private String department;
    private String academicTitle;
    private List<String> interests;
    private List<String> subjects;
    private Boolean tutorProfileEnabled;
    private boolean emailVerified;
    /** Dacă true, frontend-ul trebuie să redirecționeze utilizatorul la pagina de selectare rol. */
    private boolean needsRoleSelection;

    private boolean banned;
    private String bannedBy;
    private Date bannedAt;
    private String banReason;

    private List<String> ownedCourses;
    private List<String> enrolledCourseIds;

    private Date createdAt;
    private Date lastLoginAt;
}
