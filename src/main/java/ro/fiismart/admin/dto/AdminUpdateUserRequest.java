package ro.fiismart.admin.dto;

import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    private String displayName;
    private Boolean isAdmin;
    private Boolean banned;
    private String banReason;
}
