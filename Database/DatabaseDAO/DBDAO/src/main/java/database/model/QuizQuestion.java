package database.model;

import lombok.Builder;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class QuizQuestion {

    private ObjectId id;
    private String text;
    private String type; // "multiple_choice" | "written"
    private int points;
    @Builder.Default
    private List<String> options = new ArrayList<>();
    private int correctIdx;        // used for multiple_choice
    private String correctText;    // used for written (keyword-based grading)
    private String explanation;

    public Document toDocument() {
        return new Document()
                .append("_id", id != null ? id : new ObjectId())
                .append("text", text)
                .append("type", type)
                .append("points", points)
                .append("options", options != null ? options : new ArrayList<>())
                .append("correctIdx", correctIdx)
                .append("correctText", correctText)
                .append("explanation", explanation);
    }

    public static QuizQuestion fromDocument(Document doc) {
        if (doc == null) return null;
        return QuizQuestion.builder()
                .id(doc.getObjectId("_id"))
                .text(doc.getString("text"))
                .type(doc.getString("type"))
                .points(doc.getInteger("points", 1))
                .options(doc.getList("options", String.class) != null
                        ? doc.getList("options", String.class) : new ArrayList<>())
                .correctIdx(doc.getInteger("correctIdx", 0))
                .correctText(doc.getString("correctText"))
                .explanation(doc.getString("explanation"))
                .build();
    }
}
