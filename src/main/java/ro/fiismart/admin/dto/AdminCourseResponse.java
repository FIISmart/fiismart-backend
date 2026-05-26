package ro.fiismart.admin.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.Course;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class AdminCourseResponse {
    private String id;
    private String title;
    private String description;
    private String teacherId;
    private String status;
    private boolean hidden;
    private List<String> tags;
    private String thumbnailUrl;
    private int enrollmentCount;
    private int moduleCount;
    private Date createdAt;
    private Date updatedAt;

    public static AdminCourseResponse from(Course c) {
        return AdminCourseResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .teacherId(c.getTeacherId())
                .status(c.getStatus())
                .hidden(c.isHidden())
                .tags(c.getTags())
                .thumbnailUrl(c.getThumbnailUrl())
                .enrollmentCount(c.getEnrollmentCount())
                .moduleCount(c.getModules() != null ? c.getModules().size() : 0)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
