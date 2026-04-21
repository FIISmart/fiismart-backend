// ─────────────────────────────────────────────────────────────────
// FILE: database/dao/QuizDAO.java  (actualizat cu replaceQuestions)
// ─────────────────────────────────────────────────────────────────
package database.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import database.model.Quiz;
import database.model.QuizQuestion;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class QuizDAO {

    private final MongoCollection<Document> collection;

    public QuizDAO() {
        this.collection = MongoConnectionPool.getInstance()
                .getDatabase()
                .getCollection("Quiz");
    }

    // ── CREATE ──────────────────────────────────────────────────────────────

    public ObjectId insert(Quiz quiz) {
        Document doc = quiz.toDocument();
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    public Quiz findById(ObjectId quizId) {
        return Quiz.fromDocument(collection.find(eq("_id", quizId)).first());
    }

    public Quiz findByCourseId(ObjectId courseId) {
        return Quiz.fromDocument(collection.find(eq("courseId", courseId)).first());
    }

    public Quiz findByModuleId(ObjectId moduleId) {
        return Quiz.fromDocument(collection.find(eq("moduleId", moduleId)).first());
    }

    public boolean existsByModuleId(ObjectId moduleId) {
        return collection.countDocuments(eq("moduleId", moduleId)) > 0;
    }

    public com.mongodb.client.result.DeleteResult deleteByModuleId(ObjectId moduleId) {
        return collection.deleteOne(eq("moduleId", moduleId));
    }

    public List<QuizQuestion> findQuestions(ObjectId quizId) {
        Quiz quiz = findById(quizId);
        if (quiz == null) return new ArrayList<>();
        return quiz.getQuestions();
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    public UpdateResult updateTitle(ObjectId quizId, String title) {
        return collection.updateOne(eq("_id", quizId), set("title", title));
    }

    public UpdateResult updatePassingScore(ObjectId quizId, int passingScore) {
        return collection.updateOne(eq("_id", quizId), set("passingScore", passingScore));
    }

    public UpdateResult updateTimeLimit(ObjectId quizId, int timeLimit) {
        return collection.updateOne(eq("_id", quizId), set("timeLimit", timeLimit));
    }

    public UpdateResult updateShuffleQuestions(ObjectId quizId, boolean shuffle) {
        return collection.updateOne(eq("_id", quizId), set("shuffleQuestions", shuffle));
    }

    // ── QUESTION MANAGEMENT ──────────────────────────────────────────────────

    public UpdateResult addQuestion(ObjectId quizId, QuizQuestion question) {
        return collection.updateOne(eq("_id", quizId), push("questions", question.toDocument()));
    }

    public UpdateResult removeQuestion(ObjectId quizId, ObjectId questionId) {
        return collection.updateOne(eq("_id", quizId),
                pull("questions", new Document("_id", questionId)));
    }

    public UpdateResult updateQuestionField(ObjectId quizId, ObjectId questionId, String field, Object value) {
        return collection.updateOne(
                and(eq("_id", quizId), eq("questions._id", questionId)),
                set("questions.$." + field, value)
        );
    }

    /**
     * Înlocuiește complet lista de întrebări (folosit la reordonare drag & drop).
     */
    public UpdateResult replaceQuestions(ObjectId quizId, List<Document> questionDocs) {
        return collection.updateOne(eq("_id", quizId), set("questions", questionDocs));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    public DeleteResult deleteById(ObjectId quizId) {
        return collection.deleteOne(eq("_id", quizId));
    }

    public DeleteResult deleteByCourseId(ObjectId courseId) {
        return collection.deleteOne(eq("courseId", courseId));
    }

    // ── UTIL ─────────────────────────────────────────────────────────────────

    public boolean existsByCourseId(ObjectId courseId) {
        return collection.countDocuments(eq("courseId", courseId)) > 0;
    }
}
