package com.fiismart.backend.course.dto.response;

import database.model.Lecture;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class LectureResponse {

    private String id;
    private String title;
    private String videoUrl;
    private List<String> imageUrls;
    private int order;
    private int durationSecs;
    private Date publishedAt;

    public static LectureResponse fromModel(Lecture lecture) {
        if (lecture == null) return null;
        return LectureResponse.builder()
                .id(lecture.getId() != null ? lecture.getId().toHexString() : null)
                .title(lecture.getTitle())
                .videoUrl(lecture.getVideoUrl())
                .imageUrls(lecture.getImageUrls())
                .order(lecture.getOrder())
                .durationSecs(lecture.getDurationSecs())
                .publishedAt(lecture.getPublishedAt())
                .build();
    }
}
