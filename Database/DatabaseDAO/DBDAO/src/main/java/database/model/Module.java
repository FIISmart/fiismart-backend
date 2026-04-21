package database.model;

import lombok.Builder;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class Module {

    private ObjectId id;
    private String title;
    private String description;
    private int order;
    @Builder.Default
    private List<Lecture> lectures = new ArrayList<>();

    public Document toDocument() {
        List<Document> lectureDocs = lectures != null
                ? lectures.stream().map(Lecture::toDocument).collect(Collectors.toList())
                : new ArrayList<>();

        return new Document()
                .append("_id", id != null ? id : new ObjectId())
                .append("title", title)
                .append("description", description)
                .append("order", order)
                .append("lectures", lectureDocs);
    }

    public static Module fromDocument(Document doc) {
        if (doc == null) return null;

        List<Document> lectureDocs = doc.getList("lectures", Document.class);
        List<Lecture> lectures = lectureDocs != null
                ? lectureDocs.stream().map(Lecture::fromDocument).collect(Collectors.toList())
                : new ArrayList<>();

        return Module.builder()
                .id(doc.getObjectId("_id"))
                .title(doc.getString("title"))
                .description(doc.getString("description"))
                .order(doc.getInteger("order", 0))
                .lectures(lectures)
                .build();
    }
}
