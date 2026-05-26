package ro.fiismart.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.User;

import java.util.Date;

@Data
@Builder
public class AdminUserResponse {
    private String id;
    private String email;
    private String displayName;
    private String role;
    @JsonProperty("isAdmin")
    private boolean isAdmin;
    private boolean banned;
    private String banReason;
    private Date bannedAt;
    private boolean needsRoleSelection;
    private Date createdAt;
    private Date lastLoginAt;
    private int ownedCoursesCount;
    private int enrolledCoursesCount;

    public static AdminUserResponse from(User u) {
        return AdminUserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .displayName(u.getDisplayName())
                .role(u.getRole() != null ? u.getRole() : "student")
                .isAdmin("admin".equalsIgnoreCase(u.getRole()))
                .banned(u.isBanned())
                .banReason(u.getBanReason())
                .bannedAt(u.getBannedAt())
                .needsRoleSelection(u.isNeedsRoleSelection())
                .createdAt(u.getCreatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .ownedCoursesCount(u.getOwnedCourses() != null ? u.getOwnedCourses().size() : 0)
                .enrolledCoursesCount(u.getEnrolledCourseIds() != null ? u.getEnrolledCourseIds().size() : 0)
                .build();
    }
}
