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
public class Lecture {

    private ObjectId id;
    private String title;
    private String videoUrl;
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();
    private int order;
    private int durationSecs;
    private Date publishedAt;

    public Document toDocument() {
        return new Document()
                .append("_id", id != null ? id : new ObjectId())
                .append("title", title)
                .append("videoUrl", videoUrl)
                .append("imageUrls", imageUrls != null ? imageUrls : new ArrayList<>())
                .append("order", order)
                .append("durationSecs", durationSecs)
                .append("publishedAt", publishedAt);
    }

    public static Lecture fromDocument(Document doc) {
        if (doc == null) return null;
        return Lecture.builder()
                .id(doc.getObjectId("_id"))
                .title(doc.getString("title"))
                .videoUrl(doc.getString("videoUrl"))
                .imageUrls(doc.getList("imageUrls", String.class) != null
                        ? doc.getList("imageUrls", String.class) : new ArrayList<>())
                .order(doc.getInteger("order", 0))
                .durationSecs(doc.getInteger("durationSecs", 0))
                .publishedAt(doc.getDate("publishedAt"))
                .build();
    }
}
