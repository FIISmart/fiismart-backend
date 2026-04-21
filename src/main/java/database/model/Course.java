package database.model;

import lombok.Builder;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class Course {

    private ObjectId id;
    private String title;
    private String description;
    private ObjectId teacherId;
    private String status;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private String thumbnailUrl;
    private String language;
    private int enrollmentCount;
    private double avgRating;
    @Builder.Default
    private List<CourseModule> modules = new ArrayList<>();
    private boolean isHidden;
    private ObjectId quizId;
    private Date createdAt;
    private Date updatedAt;

    public Document toDocument() {
        List<Document> moduleDocs = modules != null
                ? modules.stream().map(CourseModule::toDocument).collect(Collectors.toList())
                : new ArrayList<>();

        return new Document()
                .append("_id", id != null ? id : new ObjectId())
                .append("title", title)
                .append("description", description)
                .append("teacherId", teacherId)
                .append("status", status)
                .append("tags", tags != null ? tags : new ArrayList<>())
                .append("thumbnailUrl", thumbnailUrl)
                .append("language", language)
                .append("enrollmentCount", enrollmentCount)
                .append("avgRating", avgRating)
                .append("modules", moduleDocs)
                .append("isHidden", isHidden)
                .append("quizId", quizId)
                .append("createdAt", createdAt)
                .append("updatedAt", updatedAt);
    }

    public static Course fromDocument(Document doc) {
        if (doc == null) return null;

        List<Document> moduleDocs = doc.getList("modules", Document.class);
        List<CourseModule> modules = moduleDocs != null
                ? moduleDocs.stream().map(CourseModule::fromDocument).collect(Collectors.toList())
                : new ArrayList<>();

        return Course.builder()
                .id(doc.getObjectId("_id"))
                .title(doc.getString("title"))
                .description(doc.getString("description"))
                .teacherId(doc.getObjectId("teacherId"))
                .status(doc.getString("status"))
                .tags(doc.getList("tags", String.class) != null
                        ? doc.getList("tags", String.class) : new ArrayList<>())
                .thumbnailUrl(doc.getString("thumbnailUrl"))
                .language(doc.getString("language"))
                .enrollmentCount(doc.getInteger("enrollmentCount", 0))
                .avgRating(doc.getDouble("avgRating") != null ? doc.getDouble("avgRating") : 0.0)
                .modules(modules)
                .isHidden(Boolean.TRUE.equals(doc.getBoolean("isHidden")))
                .quizId(doc.getObjectId("quizId"))
                .createdAt(doc.getDate("createdAt"))
                .updatedAt(doc.getDate("updatedAt"))
                .build();
    }
}