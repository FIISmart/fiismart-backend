package database.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import database.model.QuizAttempt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class QuizAttemptDAO {

    private final MongoCollection<Document> collection;

    public QuizAttemptDAO() {
        this.collection = MongoConnectionPool.getInstance()
                .getDatabase()
                .getCollection("QuizAttempt");
    }

    // ── CREATE ──────────────────────────────────────────────────────────────

    public ObjectId insert(QuizAttempt attempt) {
        Document doc = attempt.toDocument();
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    public QuizAttempt findById(ObjectId attemptId) {
        return QuizAttempt.fromDocument(collection.find(eq("_id", attemptId)).first());
    }

    public List<QuizAttempt> findByStudentId(ObjectId studentId) {
        List<Document> docs = new ArrayList<>();
        collection.find(eq("studentId", studentId)).into(docs);
        return docs.stream().map(QuizAttempt::fromDocument).collect(Collectors.toList());
    }

    public List<QuizAttempt> findByQuizId(ObjectId quizId) {
        List<Document> docs = new ArrayList<>();
        collection.find(eq("quizId", quizId)).into(docs);
        return docs.stream().map(QuizAttempt::fromDocument).collect(Collectors.toList());
    }

    public List<QuizAttempt> findByStudentAndQuiz(ObjectId studentId, ObjectId quizId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("studentId", studentId), eq("quizId", quizId))).into(docs);
        return docs.stream().map(QuizAttempt::fromDocument).collect(Collectors.toList());
    }

    public List<QuizAttempt> findByStudentAndCourse(ObjectId studentId, ObjectId courseId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("studentId", studentId), eq("courseId", courseId))).into(docs);
        return docs.stream().map(QuizAttempt::fromDocument).collect(Collectors.toList());
    }

    /** Cel mai recent attempt al unui student la un quiz */
    public QuizAttempt findLatestAttempt(ObjectId studentId, ObjectId quizId) {
        return QuizAttempt.fromDocument(collection.find(
                and(eq("studentId", studentId), eq("quizId", quizId))
        ).sort(new Document("attemptedAt", -1)).first());
    }

    public List<QuizAttempt> findPassedByQuiz(ObjectId quizId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("quizId", quizId), eq("passed", true))).into(docs);
        return docs.stream().map(QuizAttempt::fromDocument).collect(Collectors.toList());
    }

    public double computeAvgScore(ObjectId quizId) {
        List<QuizAttempt> attempts = findByQuizId(quizId);
        if (attempts.isEmpty()) return 0.0;
        double total = attempts.stream().mapToInt(QuizAttempt::getScore).sum();
        return Math.round((total / attempts.size()) * 10.0) / 10.0;
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    public UpdateResult setAnswers(ObjectId attemptId, List<Document> answers) {
        return collection.updateOne(eq("_id", attemptId), set("answers", answers));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    public DeleteResult deleteById(ObjectId attemptId) {
        return collection.deleteOne(eq("_id", attemptId));
    }

    public DeleteResult deleteAllByQuiz(ObjectId quizId) {
        return collection.deleteMany(eq("quizId", quizId));
    }

    public DeleteResult deleteAllByCourse(ObjectId courseId) {
        return collection.deleteMany(eq("courseId", courseId));
    }

    public DeleteResult deleteAllByStudent(ObjectId studentId) {
        return collection.deleteMany(eq("studentId", studentId));
    }

    // ── UTIL ─────────────────────────────────────────────────────────────────

    public boolean hasStudentPassedQuiz(ObjectId studentId, ObjectId quizId) {
        return collection.countDocuments(and(
                eq("studentId", studentId),
                eq("quizId", quizId),
                eq("passed", true)
        )) > 0;
    }

    public long countByStudentAndQuiz(ObjectId studentId, ObjectId quizId) {
        return collection.countDocuments(and(eq("studentId", studentId), eq("quizId", quizId)));
    }

    public long countPassedByQuiz(ObjectId quizId) {
        return collection.countDocuments(and(eq("quizId", quizId), eq("passed", true)));
    }
}
