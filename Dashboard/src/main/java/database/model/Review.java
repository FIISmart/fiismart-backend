package database.model;

import lombok.Builder;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Date;

@Data
@Builder
public class Review {

    private ObjectId id;
    private ObjectId studentId;
    private ObjectId courseId;
    private int stars;
    private String body;
    private Date createdAt;
    private boolean isDeleted;
    private ObjectId deletedBy;

    public Document toDocument() {
        return new Document()
                .append("_id", id != null ? id : new ObjectId())
                .append("studentId", studentId)
                .append("courseId", courseId)
                .append("stars", stars)
                .append("body", body)
                .append("createdAt", createdAt)
                .append("isDeleted", isDeleted)
                .append("deletedBy", deletedBy);
    }

    public static Review fromDocument(Document doc) {
        if (doc == null) return null;
        return Review.builder()
                .id(doc.getObjectId("_id"))
                .studentId(doc.getObjectId("studentId"))
                .courseId(doc.getObjectId("courseId"))
                .stars(doc.getInteger("stars", 0))
                .body(doc.getString("body"))
                .createdAt(doc.getDate("createdAt"))
                .isDeleted(Boolean.TRUE.equals(doc.getBoolean("isDeleted")))
                .deletedBy(doc.getObjectId("deletedBy"))
                .build();
    }
}
