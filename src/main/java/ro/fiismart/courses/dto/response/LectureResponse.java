package ro.fiismart.courses.dto.response;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.Lecture;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class LectureResponse {

    private String id;
    private String moduleId;
    private String title;
    private String videoUrl;
    private List<String> imageUrls;
    private int order;
    private int durationSecs;
    private Date publishedAt;

    public static LectureResponse fromModel(Lecture lecture) {
        if (lecture == null) return null;
        return LectureResponse.builder()
                .id(lecture.getId())
                .moduleId(lecture.getModuleId())
                .title(lecture.getTitle())
                .videoUrl(lecture.getVideoUrl())
                .imageUrls(lecture.getImageUrls())
                .order(lecture.getOrder())
                .durationSecs(lecture.getDurationSecs())
                .publishedAt(lecture.getPublishedAt())
                .build();
    }
}
