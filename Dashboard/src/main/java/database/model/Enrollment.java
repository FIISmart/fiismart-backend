package database.model;

import lombok.Builder;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class Enrollment {

    private ObjectId id;
    private ObjectId studentId;
    private ObjectId courseId;
    private Date enrolledAt;
    private Date completedAt;
    private String status; // "enrolled" | "completed"
    @Builder.Default
    private List<Document> lectureProgress = new ArrayList<>();
    private Date lastAccessedAt;
    private int overallProgress;

    public Document toDocument() {
        return new Document()
                .append("_id", id != null ? id : new ObjectId())
                .append("studentId", studentId)
                .append("courseId", courseId)
                .append("enrolledAt", enrolledAt)
                .append("completedAt", completedAt)
                .append("status", status)
                .append("lectureProgress", lectureProgress != null ? lectureProgress : new ArrayList<>())
                .append("lastAccessedAt", lastAccessedAt)
                .append("overallProgress", overallProgress);
    }

    public static Enrollment fromDocument(Document doc) {
        if (doc == null) return null;
        return Enrollment.builder()
                .id(doc.getObjectId("_id"))
                .studentId(doc.getObjectId("studentId"))
                .courseId(doc.getObjectId("courseId"))
                .enrolledAt(doc.getDate("enrolledAt"))
                .completedAt(doc.getDate("completedAt"))
                .status(doc.getString("status"))
                .lectureProgress(doc.getList("lectureProgress", Document.class) != null
                        ? doc.getList("lectureProgress", Document.class) : new ArrayList<>())
                .lastAccessedAt(doc.getDate("lastAccessedAt"))
                .overallProgress(doc.getInteger("overallProgress", 0))
                .build();
    }
}
