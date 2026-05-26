package ro.fiismart.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminUpdateCourseRequest {
    private String title;
    private String description;
    private String status;
    private Boolean hidden;
    private List<String> tags;
}
