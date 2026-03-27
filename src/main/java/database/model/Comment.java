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
public class Comment {

    private ObjectId id;
    private ObjectId lectureId;
    private ObjectId courseId;
    private ObjectId authorId;
    private String body;
    private Date createdAt;
    private Date updatedAt;
    private boolean isDeleted;
    private ObjectId parentCommentId; // null = top-level
    private int likeCount;
    @Builder.Default
    private List<ObjectId> likedBy = new ArrayList<>();
    @Builder.Default
    private List<Document> moderationFlags = new ArrayList<>();
    private int flagCount;

    public Document toDocument() {
        return new Document()
                .append("_id", id != null ? id : new ObjectId())
                .append("lectureId", lectureId)
                .append("courseId", courseId)
                .append("authorId", authorId)
                .append("body", body)
                .append("createdAt", createdAt)
                .append("updatedAt", updatedAt)
                .append("isDeleted", isDeleted)
                .append("parentCommentId", parentCommentId)
                .append("likeCount", likeCount)
                .append("likedBy", likedBy != null ? likedBy : new ArrayList<>())
                .append("moderationFlags", moderationFlags != null ? moderationFlags : new ArrayList<>())
                .append("flagCount", flagCount);
    }

    public static Comment fromDocument(Document doc) {
        if (doc == null) return null;
        return Comment.builder()
                .id(doc.getObjectId("_id"))
                .lectureId(doc.getObjectId("lectureId"))
                .courseId(doc.getObjectId("courseId"))
                .authorId(doc.getObjectId("authorId"))
                .body(doc.getString("body"))
                .createdAt(doc.getDate("createdAt"))
                .updatedAt(doc.getDate("updatedAt"))
                .isDeleted(Boolean.TRUE.equals(doc.getBoolean("isDeleted")))
                .parentCommentId(doc.getObjectId("parentCommentId"))
                .likeCount(doc.getInteger("likeCount", 0))
                .likedBy(doc.getList("likedBy", ObjectId.class) != null
                        ? doc.getList("likedBy", ObjectId.class) : new ArrayList<>())
                .moderationFlags(doc.getList("moderationFlags", Document.class) != null
                        ? doc.getList("moderationFlags", Document.class) : new ArrayList<>())
                .flagCount(doc.getInteger("flagCount", 0))
                .build();
    }
}
