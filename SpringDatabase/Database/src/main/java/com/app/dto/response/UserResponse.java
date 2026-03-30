package com.app.dto.response;

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
    private String displayName;
    private String email;
    private String role;
    private boolean banned;
    private String bannedBy;
    private Date bannedAt;
    private String banReason;
    private List<String> ownedCourses;
    private List<String> enrolledCourseIds;
    private Date createdAt;
    private Date lastLoginAt;
}
