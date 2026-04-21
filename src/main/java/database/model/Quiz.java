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
public class Quiz {

    private ObjectId id;
    private ObjectId courseId;
    private ObjectId moduleId;   // nullable — null = legacy course-wide quiz
    private String title;
    private int passingScore;
    private int timeLimit;
    private boolean shuffleQuestions;
    @Builder.Default
    private List<QuizQuestion> questions = new ArrayList<>();

    public Document toDocument() {
        List<Document> questionDocs = questions != null
                ? questions.stream().map(QuizQuestion::toDocument).collect(Collectors.toList())
                : new ArrayList<>();

        return new Document()
                .append("_id", id != null ? id : new ObjectId())
                .append("courseId", courseId)
                .append("moduleId", moduleId)
                .append("title", title)
                .append("passingScore", passingScore)
                .append("timeLimit", timeLimit)
                .append("shuffleQuestions", shuffleQuestions)
                .append("questions", questionDocs);
    }

    public static Quiz fromDocument(Document doc) {
        if (doc == null) return null;

        List<Document> questionDocs = doc.getList("questions", Document.class);
        List<QuizQuestion> questions = questionDocs != null
                ? questionDocs.stream().map(QuizQuestion::fromDocument).collect(Collectors.toList())
                : new ArrayList<>();

        return Quiz.builder()
                .id(doc.getObjectId("_id"))
                .courseId(doc.getObjectId("courseId"))
                .moduleId(doc.getObjectId("moduleId"))
                .title(doc.getString("title"))
                .passingScore(doc.getInteger("passingScore", 70))
                .timeLimit(doc.getInteger("timeLimit", 30))
                .shuffleQuestions(Boolean.TRUE.equals(doc.getBoolean("shuffleQuestions")))
                .questions(questions)
                .build();
    }
}
