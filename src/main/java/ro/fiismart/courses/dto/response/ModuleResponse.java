package ro.fiismart.courses.dto.response;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.CourseModule;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class ModuleResponse {

    private String id;
    private String title;
    private String description;
    private int order;
    private String quizId;
    private List<LectureResponse> lectures;

    public static ModuleResponse fromModel(CourseModule module) {
        if (module == null) return null;
        return ModuleResponse.builder()
                .id(module.getId())
                .title(module.getTitle())
                .description(module.getDescription())
                .order(module.getOrder())
                .quizId(module.getQuizId())
                .lectures(module.getLectures() != null
                        ? module.getLectures().stream().map(LectureResponse::fromModel).collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
