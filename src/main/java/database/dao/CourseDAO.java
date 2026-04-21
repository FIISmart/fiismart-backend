package database.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import database.model.CourseModule;
import org.bson.Document;
import org.bson.types.ObjectId;
import database.model.Course;
import database.model.Lecture;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class CourseDAO {

    private final MongoCollection<Document> collection;

    public CourseDAO() {
        this.collection = MongoConnectionPool.getInstance()
                .getDatabase()
                .getCollection("Courses");
    }

    // ── CREATE ──────────────────────────────────────────────────────────────

    public ObjectId insert(Course course) {
        Document doc = course.toDocument();
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    public Course findById(ObjectId courseId) {
        return Course.fromDocument(collection.find(eq("_id", courseId)).first());
    }

    public List<Course> findAll() {
        List<Document> docs = new ArrayList<>();
        collection.find().into(docs);
        return docs.stream().map(Course::fromDocument).collect(Collectors.toList());
    }

    public List<Course> findByTeacherId(ObjectId teacherId) {
        List<Document> docs = new ArrayList<>();
        collection.find(eq("teacherId", teacherId)).into(docs);
        return docs.stream().map(Course::fromDocument).collect(Collectors.toList());
    }

    public List<Course> findPublishedVisible() {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("status", "published"), eq("isHidden", false))).into(docs);
        return docs.stream().map(Course::fromDocument).collect(Collectors.toList());
    }

    public List<Course> findByTag(String tag) {
        List<Document> docs = new ArrayList<>();
        collection.find(eq("tags", tag)).into(docs);
        return docs.stream().map(Course::fromDocument).collect(Collectors.toList());
    }

    public List<Course> findByMinRating(double minRating) {
        List<Document> docs = new ArrayList<>();
        collection.find(gte("avgRating", minRating)).into(docs);
        return docs.stream().map(Course::fromDocument).collect(Collectors.toList());
    }

    //modificat
    public List<Lecture> findLecturesByCourseId(ObjectId courseId) {
        Course course = findById(courseId);
        if (course == null) return new ArrayList<>();

        List<Lecture> allLectures = new ArrayList<>();
        if (course.getModules() != null) {
            for (CourseModule module : course.getModules()) {
                if (module.getLectures() != null) {
                    allLectures.addAll(module.getLectures());
                }
            }
        }
        return allLectures;
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    public UpdateResult updateTitle(ObjectId courseId, String newTitle) {
        return collection.updateOne(eq("_id", courseId), set("title", newTitle));
    }

    public UpdateResult updateDescription(ObjectId courseId, String description) {
        return collection.updateOne(eq("_id", courseId), set("description", description));
    }

    public UpdateResult updateStatus(ObjectId courseId, String status) {
        return collection.updateOne(eq("_id", courseId), set("status", status));
    }

    public UpdateResult setHidden(ObjectId courseId, boolean isHidden) {
        return collection.updateOne(eq("_id", courseId), set("isHidden", isHidden));
    }

    public UpdateResult setQuizId(ObjectId courseId, ObjectId quizId) {
        return collection.updateOne(eq("_id", courseId), set("quizId", quizId));
    }

    public UpdateResult incrementEnrollmentCount(ObjectId courseId) {
        return collection.updateOne(eq("_id", courseId), inc("enrollmentCount", 1));
    }

    public UpdateResult decrementEnrollmentCount(ObjectId courseId) {
        return collection.updateOne(eq("_id", courseId), inc("enrollmentCount", -1));
    }

    public UpdateResult updateAvgRating(ObjectId courseId, double avgRating) {
        return collection.updateOne(eq("_id", courseId), set("avgRating", avgRating));
    }

    public UpdateResult updateUpdatedAt(ObjectId courseId, Date updatedAt) {
        return collection.updateOne(eq("_id", courseId), set("updatedAt", updatedAt));
    }

    // ── LECTURE MANAGEMENT ──────────────────────────────────────────────────

    public UpdateResult addLecture(ObjectId courseId, Lecture lecture) {
        return collection.updateOne(eq("_id", courseId), push("lectures", lecture.toDocument()));
    }

    public UpdateResult removeLecture(ObjectId courseId, ObjectId lectureId) {
        return collection.updateOne(eq("_id", courseId),
                pull("lectures", new Document("_id", lectureId)));
    }

    /** Actualizează un câmp dintr-o lectură folosind operatorul pozițional $ */
    public UpdateResult updateLectureField(ObjectId courseId, ObjectId lectureId, String field, Object value) {
        return collection.updateOne(
                and(eq("_id", courseId), eq("lectures._id", lectureId)),
                set("lectures.$." + field, value)
        );
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    public DeleteResult deleteById(ObjectId courseId) {
        return collection.deleteOne(eq("_id", courseId));
    }

    // ── UTIL ─────────────────────────────────────────────────────────────────

    public boolean existsById(ObjectId courseId) {
        return collection.countDocuments(eq("_id", courseId)) > 0;
    }

    public long countByTeacher(ObjectId teacherId) {
        return collection.countDocuments(eq("teacherId", teacherId));
    }
}
