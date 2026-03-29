package database.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import database.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class UserDAO {

    private final MongoCollection<Document> collection;

    public UserDAO() {
        this.collection = MongoConnectionPool.getInstance()
                .getDatabase()
                .getCollection("Users");
    }

    // ── CREATE ──────────────────────────────────────────────────────────────

    public ObjectId insert(User user) {
        Document doc = user.toDocument();
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    public User findById(ObjectId id) {
        return User.fromDocument(collection.find(eq("_id", id)).first());
    }

    public User findByEmail(String email) {
        return User.fromDocument(collection.find(eq("email", email)).first());
    }

    public List<User> findAllByRole(String role) {
        List<Document> docs = new ArrayList<>();
        collection.find(eq("role", role)).into(docs);
        return docs.stream().map(User::fromDocument).collect(Collectors.toList());
    }

    public List<User> findAllTeachers() { return findAllByRole("teacher"); }
    public List<User> findAllStudents() { return findAllByRole("student"); }
    public List<User> findAllAdmins()   { return findAllByRole("admin"); }

    public List<User> findBannedUsers() {
        List<Document> docs = new ArrayList<>();
        collection.find(eq("banned", true)).into(docs);
        return docs.stream().map(User::fromDocument).collect(Collectors.toList());
    }

    public List<User> findStudentsEnrolledInCourse(ObjectId courseId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("role", "student"), eq("enrolledCourseIds", courseId))).into(docs);
        return docs.stream().map(User::fromDocument).collect(Collectors.toList());
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    public UpdateResult updateLastLogin(ObjectId userId, Date loginTime) {
        return collection.updateOne(eq("_id", userId), set("lastLoginAt", loginTime));
    }

    public UpdateResult updateDisplayName(ObjectId userId, String newName) {
        return collection.updateOne(eq("_id", userId), set("displayName", newName));
    }

    public UpdateResult updatePasswordHash(ObjectId userId, String newHash) {
        return collection.updateOne(eq("_id", userId), set("passwordHash", newHash));
    }

    public UpdateResult banUser(ObjectId userId, ObjectId bannedByAdminId, String reason, Date bannedAt) {
        return collection.updateOne(eq("_id", userId), combine(
                set("banned", true),
                set("bannedBy", bannedByAdminId),
                set("bannedAt", bannedAt),
                set("banReason", reason)
        ));
    }

    public UpdateResult unbanUser(ObjectId userId) {
        return collection.updateOne(eq("_id", userId), combine(
                set("banned", false),
                set("bannedBy", null),
                set("bannedAt", null),
                set("banReason", null)
        ));
    }

    public UpdateResult addOwnedCourse(ObjectId teacherId, ObjectId courseId) {
        return collection.updateOne(eq("_id", teacherId), addToSet("ownedCourses", courseId));
    }

    public UpdateResult removeOwnedCourse(ObjectId teacherId, ObjectId courseId) {
        return collection.updateOne(eq("_id", teacherId), pull("ownedCourses", courseId));
    }

    public UpdateResult addEnrolledCourse(ObjectId studentId, ObjectId courseId) {
        return collection.updateOne(eq("_id", studentId), addToSet("enrolledCourseIds", courseId));
    }

    public UpdateResult removeEnrolledCourse(ObjectId studentId, ObjectId courseId) {
        return collection.updateOne(eq("_id", studentId), pull("enrolledCourseIds", courseId));
    }

    public UpdateResult addSession(ObjectId studentId, Document session) {
        return collection.updateOne(eq("_id", studentId), push("sessions", session));
    }

    public UpdateResult removeSession(ObjectId studentId, String sessionToken) {
        return collection.updateOne(eq("_id", studentId),
                pull("sessions", new Document("token", sessionToken)));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    public DeleteResult deleteById(ObjectId userId) {
        return collection.deleteOne(eq("_id", userId));
    }

    // ── UTIL ─────────────────────────────────────────────────────────────────

    public boolean existsByEmail(String email) {
        return collection.countDocuments(eq("email", email)) > 0;
    }

    public boolean isEnrolledInCourse(ObjectId studentId, ObjectId courseId) {
        return collection.countDocuments(and(
                eq("_id", studentId), eq("enrolledCourseIds", courseId)
        )) > 0;
    }

    public long countByRole(String role) {
        return collection.countDocuments(eq("role", role));
    }
}
