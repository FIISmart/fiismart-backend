package com.fiismart.backend.course.dto.response;

import database.model.Module;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class ModuleResponse {

    private String id;
    private String title;
    private String description;
    private int order;
    private List<LectureResponse> lectures;

    public static ModuleResponse fromModel(Module module) {
        if (module == null) return null;
        return ModuleResponse.builder()
                .id(module.getId() != null ? module.getId().toHexString() : null)
                .title(module.getTitle())
                .description(module.getDescription())
                .order(module.getOrder())
                .lectures(module.getLectures() != null
                        ? module.getLectures().stream()
                        .map(LectureResponse::fromModel)
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
