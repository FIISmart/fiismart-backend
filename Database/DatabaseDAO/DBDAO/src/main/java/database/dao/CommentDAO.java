package database.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import database.model.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class CommentDAO {

    private final MongoCollection<Document> collection;

    public CommentDAO() {
        this.collection = MongoConnectionPool.getInstance()
                .getDatabase()
                .getCollection("Comments");
    }

    // ── CREATE ──────────────────────────────────────────────────────────────

    public ObjectId insert(Comment comment) {
        Document doc = comment.toDocument();
        collection.insertOne(doc);
        return doc.getObjectId("_id");
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    public Comment findById(ObjectId commentId) {
        return Comment.fromDocument(collection.find(eq("_id", commentId)).first());
    }

    public List<Comment> findByLectureId(ObjectId lectureId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("lectureId", lectureId), eq("isDeleted", false))).into(docs);
        return docs.stream().map(Comment::fromDocument).collect(Collectors.toList());
    }

    public List<Comment> findTopLevelByLectureId(ObjectId lectureId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(
                eq("lectureId", lectureId),
                eq("parentCommentId", null),
                eq("isDeleted", false)
        )).into(docs);
        return docs.stream().map(Comment::fromDocument).collect(Collectors.toList());
    }

    public List<Comment> findRepliesByParentId(ObjectId parentCommentId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("parentCommentId", parentCommentId), eq("isDeleted", false))).into(docs);
        return docs.stream().map(Comment::fromDocument).collect(Collectors.toList());
    }

    public List<Comment> findByAuthorId(ObjectId authorId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("authorId", authorId), eq("isDeleted", false))).into(docs);
        return docs.stream().map(Comment::fromDocument).collect(Collectors.toList());
    }

    public List<Comment> findFlagged(int minFlags) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(gte("flagCount", minFlags), eq("isDeleted", false))).into(docs);
        return docs.stream().map(Comment::fromDocument).collect(Collectors.toList());
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    public UpdateResult updateBody(ObjectId commentId, String newBody, Date updatedAt) {
        return collection.updateOne(eq("_id", commentId), combine(
                set("body", newBody),
                set("updatedAt", updatedAt)
        ));
    }

    public UpdateResult softDelete(ObjectId commentId) {
        return collection.updateOne(eq("_id", commentId), set("isDeleted", true));
    }

    public UpdateResult addLike(ObjectId commentId, ObjectId userId) {
        return collection.updateOne(
                and(eq("_id", commentId), nin("likedBy", userId)),
                combine(addToSet("likedBy", userId), inc("likeCount", 1))
        );
    }

    public UpdateResult removeLike(ObjectId commentId, ObjectId userId) {
        return collection.updateOne(
                and(eq("_id", commentId), eq("likedBy", userId)),
                combine(pull("likedBy", userId), inc("likeCount", -1))
        );
    }

    /**
     * flag document: { flaggedBy: ObjectId, reason: String, flaggedAt: Date }
     */
    public UpdateResult addModerationFlag(ObjectId commentId, Document flag) {
        return collection.updateOne(eq("_id", commentId),
                combine(push("moderationFlags", flag), inc("flagCount", 1)));
    }

    public UpdateResult clearModerationFlags(ObjectId commentId) {
        return collection.updateOne(eq("_id", commentId), combine(
                set("moderationFlags", new ArrayList<>()),
                set("flagCount", 0)
        ));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    public DeleteResult deleteById(ObjectId commentId) {
        return collection.deleteOne(eq("_id", commentId));
    }

    public DeleteResult deleteAllByLecture(ObjectId lectureId) {
        return collection.deleteMany(eq("lectureId", lectureId));
    }

    public DeleteResult deleteAllByCourse(ObjectId courseId) {
        return collection.deleteMany(eq("courseId", courseId));
    }

    // ── UTIL ─────────────────────────────────────────────────────────────────

    public long countByLecture(ObjectId lectureId) {
        return collection.countDocuments(and(eq("lectureId", lectureId), eq("isDeleted", false)));
    }

    public boolean hasUserLiked(ObjectId commentId, ObjectId userId) {
        return collection.countDocuments(and(eq("_id", commentId), eq("likedBy", userId))) > 0;
    }
}
