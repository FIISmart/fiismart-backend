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
public class QuizAttempt {

    private ObjectId id;
    private ObjectId quizId;
    private ObjectId courseId;
    private ObjectId studentId;
    private Date attemptedAt;
    private int score;
    private boolean passed;
    private int timeTakenSecs;
    @Builder.Default
    private List<Document> answers = new ArrayList<>();

    public Document toDocument() {
        return new Document()
                .append("_id", id != null ? id : new ObjectId())
                .append("quizId", quizId)
                .append("courseId", courseId)
                .append("studentId", studentId)
                .append("attemptedAt", attemptedAt)
                .append("score", score)
                .append("passed", passed)
                .append("timeTakenSecs", timeTakenSecs)
                .append("answers", answers != null ? answers : new ArrayList<>());
    }

    public static QuizAttempt fromDocument(Document doc) {
        if (doc == null) return null;
        return QuizAttempt.builder()
                .id(doc.getObjectId("_id"))
                .quizId(doc.getObjectId("quizId"))
                .courseId(doc.getObjectId("courseId"))
                .studentId(doc.getObjectId("studentId"))
                .attemptedAt(doc.getDate("attemptedAt"))
                .score(doc.getInteger("score", 0))
                .passed(Boolean.TRUE.equals(doc.getBoolean("passed")))
                .timeTakenSecs(doc.getInteger("timeTakenSecs", 0))
                .answers(doc.getList("answers", Document.class) != null
                        ? doc.getList("answers", Document.class) : new ArrayList<>())
                .build();
    }
}
