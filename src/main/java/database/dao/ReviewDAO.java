package database.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import database.model.Review;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class ReviewDAO {

    private final MongoCollection<Document> collection;

    public ReviewDAO() {
        this.collection = MongoConnectionPool.getInstance()
                .getDatabase()
                .getCollection("Review");
    }

    // ── CREATE ──────────────────────────────────────────────────────────────

    public ObjectId insert(Review review) {
        Document doc = review.toDocument();
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    public Review findById(ObjectId reviewId) {
        return Review.fromDocument(collection.find(eq("_id", reviewId)).first());
    }

    public Review findByStudentAndCourse(ObjectId studentId, ObjectId courseId) {
        return Review.fromDocument(collection.find(and(
                eq("studentId", studentId),
                eq("courseId", courseId),
                eq("isDeleted", false)
        )).first());
    }

    public List<Review> findByCourseId(ObjectId courseId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("courseId", courseId), eq("isDeleted", false))).into(docs);
        return docs.stream().map(Review::fromDocument).collect(Collectors.toList());
    }

    public List<Review> findByStudentId(ObjectId studentId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("studentId", studentId), eq("isDeleted", false))).into(docs);
        return docs.stream().map(Review::fromDocument).collect(Collectors.toList());
    }

    public List<Review> findByCourseAndStars(ObjectId courseId, int stars) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("courseId", courseId), eq("stars", stars), eq("isDeleted", false))).into(docs);
        return docs.stream().map(Review::fromDocument).collect(Collectors.toList());
    }

    public double computeAvgRating(ObjectId courseId) {
        List<Review> reviews = findByCourseId(courseId);
        if (reviews.isEmpty()) return 0.0;
        double total = reviews.stream().mapToInt(Review::getStars).sum();
        return Math.round((total / reviews.size()) * 10.0) / 10.0;
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    public UpdateResult updateReview(ObjectId reviewId, int newStars, String newBody) {
        return collection.updateOne(eq("_id", reviewId), combine(
                set("stars", newStars),
                set("body", newBody)
        ));
    }

    public UpdateResult softDelete(ObjectId reviewId, ObjectId deletedByUserId) {
        return collection.updateOne(eq("_id", reviewId), combine(
                set("isDeleted", true),
                set("deletedBy", deletedByUserId)
        ));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    public DeleteResult deleteById(ObjectId reviewId) {
        return collection.deleteOne(eq("_id", reviewId));
    }

    public DeleteResult deleteAllByCourse(ObjectId courseId) {
        return collection.deleteMany(eq("courseId", courseId));
    }

    // ── UTIL ─────────────────────────────────────────────────────────────────

    public boolean hasStudentReviewedCourse(ObjectId studentId, ObjectId courseId) {
        return collection.countDocuments(and(
                eq("studentId", studentId),
                eq("courseId", courseId),
                eq("isDeleted", false)
        )) > 0;
    }

    public long countByCourse(ObjectId courseId) {
        return collection.countDocuments(and(eq("courseId", courseId), eq("isDeleted", false)));
    }
}
