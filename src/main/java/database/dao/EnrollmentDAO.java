package database.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import database.model.Enrollment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class EnrollmentDAO {

    private final MongoCollection<Document> collection;

    public EnrollmentDAO() {
        this.collection = MongoConnectionPool.getInstance()
                .getDatabase()
                .getCollection("Enrollments");
    }

    // ── CREATE ──────────────────────────────────────────────────────────────

    public ObjectId insert(Enrollment enrollment) {
        Document doc = enrollment.toDocument();
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    public Enrollment findById(ObjectId enrollmentId) {
        return Enrollment.fromDocument(collection.find(eq("_id", enrollmentId)).first());
    }

    public Enrollment findByStudentAndCourse(ObjectId studentId, ObjectId courseId) {
        return Enrollment.fromDocument(collection.find(and(
                eq("studentId", studentId), eq("courseId", courseId)
        )).first());
    }

    public List<Enrollment> findByStudentId(ObjectId studentId) {
        List<Document> docs = new ArrayList<>();
        collection.find(eq("studentId", studentId)).into(docs);
        return docs.stream().map(Enrollment::fromDocument).collect(Collectors.toList());
    }

    public List<Enrollment> findByCourseId(ObjectId courseId) {
        List<Document> docs = new ArrayList<>();
        collection.find(eq("courseId", courseId)).into(docs);
        return docs.stream().map(Enrollment::fromDocument).collect(Collectors.toList());
    }

    public List<Enrollment> findCompletedByStudent(ObjectId studentId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("studentId", studentId), eq("status", "completed"))).into(docs);
        return docs.stream().map(Enrollment::fromDocument).collect(Collectors.toList());
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    public UpdateResult updateStatus(ObjectId enrollmentId, String status) {
        return collection.updateOne(eq("_id", enrollmentId), set("status", status));
    }

    public UpdateResult updateOverallProgress(ObjectId enrollmentId, int progress) {
        return collection.updateOne(eq("_id", enrollmentId), set("overallProgress", progress));
    }

    public UpdateResult updateLastAccessedAt(ObjectId enrollmentId, Date lastAccessedAt) {
        return collection.updateOne(eq("_id", enrollmentId), set("lastAccessedAt", lastAccessedAt));
    }

    public UpdateResult markCompleted(ObjectId enrollmentId, Date completedAt) {
        return collection.updateOne(eq("_id", enrollmentId), combine(
                set("status", "completed"),
                set("completedAt", completedAt),
                set("overallProgress", 100)
        ));
    }

    /**
     * Upsert pe element din lectureProgress.
     * Dacă există element cu acest lectureId → înlocuiește tot documentul.
     * Dacă nu există → face push.
     *
     * Documentul progress ar trebui să aibă forma:
     * { lectureId, moduleId, watchedPercent, positionSecs, completed, updatedAt }
     */
    public UpdateResult upsertLectureProgress(ObjectId enrollmentId, ObjectId lectureId, Document progress) {
        UpdateResult result = collection.updateOne(
                and(eq("_id", enrollmentId), eq("lectureProgress.lectureId", lectureId)),
                set("lectureProgress.$", progress)
        );

        if (result.getMatchedCount() == 0) {
            return collection.updateOne(
                    eq("_id", enrollmentId),
                    push("lectureProgress", progress)
            );
        }

        return result;
    }

    /**
     * lectureProgress document: { lectureId, watchedSecs, completed, lastWatchedAt }
     */
    public UpdateResult addLectureProgress(ObjectId enrollmentId, Document lectureProgress) {
        return collection.updateOne(eq("_id", enrollmentId), push("lectureProgress", lectureProgress));
    }

    public UpdateResult updateLectureProgressField(ObjectId enrollmentId, ObjectId lectureId, String field, Object value) {
        return collection.updateOne(
                and(eq("_id", enrollmentId), eq("lectureProgress.lectureId", lectureId)),
                set("lectureProgress.$." + field, value)
        );
    }

    public UpdateResult removeLectureProgress(ObjectId enrollmentId, ObjectId lectureId) {
        return collection.updateOne(eq("_id", enrollmentId),
                pull("lectureProgress", new Document("lectureId", lectureId)));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    public DeleteResult deleteById(ObjectId enrollmentId) {
        return collection.deleteOne(eq("_id", enrollmentId));
    }

    public DeleteResult deleteByStudentAndCourse(ObjectId studentId, ObjectId courseId) {
        return collection.deleteOne(and(eq("studentId", studentId), eq("courseId", courseId)));
    }

    public DeleteResult deleteAllByCourse(ObjectId courseId) {
        return collection.deleteMany(eq("courseId", courseId));
    }

    // ── UTIL ─────────────────────────────────────────────────────────────────

    public boolean isEnrolled(ObjectId studentId, ObjectId courseId) {
        return collection.countDocuments(and(
                eq("studentId", studentId), eq("courseId", courseId)
        )) > 0;
    }

    public long countByCourse(ObjectId courseId) {
        return collection.countDocuments(eq("courseId", courseId));
    }

    public long countCompletedByCourse(ObjectId courseId) {
        return collection.countDocuments(and(eq("courseId", courseId), eq("status", "completed")));
    }
}
