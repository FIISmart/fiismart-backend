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
    private String type;
    private String content;
    private String videoUrl;
    private String pdfUrl;
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
                .type(resolveType(lecture))
                .content(resolveContent(lecture))
                .videoUrl(lecture.getVideoUrl())
                .pdfUrl(resolvePdfUrl(lecture))
                .imageUrls(lecture.getImageUrls())
                .order(lecture.getOrder())
                .durationSecs(lecture.getDurationSecs())
                .publishedAt(lecture.getPublishedAt())
                .build();
    }

    private static String resolveType(Lecture lecture) {
        if (lecture.getType() != null && !lecture.getType().isBlank()) {
            return lecture.getType();
        }
        String content = resolveContent(lecture).toLowerCase();
        if (content.endsWith(".pdf") || lecture.getPdfUrl() != null) return "pdf";
        if (content.endsWith(".md") || content.endsWith(".markdown") || (!content.startsWith("http") && !content.isBlank())) {
            return "markdown";
        }
        return "video";
    }

    private static String resolveContent(Lecture lecture) {
        if (lecture.getContent() != null) return lecture.getContent();
        if (lecture.getPdfUrl() != null) return lecture.getPdfUrl();
        return lecture.getVideoUrl() != null ? lecture.getVideoUrl() : "";
    }

    private static String resolvePdfUrl(Lecture lecture) {
        if (lecture.getPdfUrl() != null) return lecture.getPdfUrl();
        String content = resolveContent(lecture);
        return "pdf".equals(resolveType(lecture)) ? content : null;
    }
}
