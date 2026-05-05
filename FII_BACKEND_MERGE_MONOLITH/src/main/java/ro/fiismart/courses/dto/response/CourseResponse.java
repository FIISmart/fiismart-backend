package ro.fiismart.courses.dto.response;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class CourseResponse {

    private String id;
    private String title;
    private String description;
    private String teacherId;
    private String status;
    private List<String> tags;
    private String thumbnailUrl;
    private String language;
    private int enrollmentCount;
    private double avgRating;
    private boolean hidden;
    private String quizId;
    private Date createdAt;
    private Date updatedAt;
    private List<ModuleResponse> modules;

    public static CourseResponse fromModel(Course course) {
        if (course == null) return null;
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .teacherId(course.getTeacherId())
                .status(course.getStatus())
                .tags(course.getTags())
                .thumbnailUrl(course.getThumbnailUrl())
                .language(course.getLanguage())
                .enrollmentCount(course.getEnrollmentCount())
                .avgRating(course.getAvgRating())
                .hidden(course.isHidden())
                .quizId(course.getQuizId())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .modules(course.getModules() != null
                        ? course.getModules().stream().map(ModuleResponse::fromModel).collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
