package com.fiismart.backend.course.helper;

import com.mongodb.client.MongoCollection;
import database.dao.MongoConnectionPool;
import database.model.Comment;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@Component
public class CommentQueryHelper {

    private final MongoCollection<Document> collection;

    public CommentQueryHelper() {
        this.collection = MongoConnectionPool.getInstance()
                .getDatabase()
                .getCollection("Comments");
    }

    /** All non-deleted comments on a course (for moderation approved/pending tabs). */
    public List<Comment> findApprovedOrPendingByCourseId(ObjectId courseId) {
        List<Document> docs = new ArrayList<>();
        collection.find(new Document("courseId", courseId).append("isDeleted", false)).into(docs);
        return docs.stream().map(Comment::fromDocument).collect(Collectors.toList());
    }

    /** All comments on a course regardless of state (moderation view needs rejected too). */
    public List<Comment> findAllByCourseId(ObjectId courseId) {
        List<Document> docs = new ArrayList<>();
        collection.find(new Document("courseId", courseId)).into(docs);
        return docs.stream().map(Comment::fromDocument).collect(Collectors.toList());
    }

    // Backwards-compat alias — the earlier signature.
    public List<Comment> findByCourseId(ObjectId courseId) {
        return findAllByCourseId(courseId);
    }

    /** Approve: un-delete, clear moderation flags. */
    public void markApproved(ObjectId commentId) {
        collection.updateOne(eq("_id", commentId), combine(
                set("isDeleted", false),
                set("moderationFlags", new ArrayList<>()),
                set("flagCount", 0)
        ));
    }

    /** Pending: un-delete, set a synthetic system flag so flagCount > 0. */
    public void markPending(ObjectId commentId) {
        Document flag = new Document()
                .append("flaggedBy", null)
                .append("reason", "manual-review")
                .append("flaggedAt", new Date());
        collection.updateOne(eq("_id", commentId), combine(
                set("isDeleted", false),
                set("moderationFlags", List.of(flag)),
                set("flagCount", 1)
        ));
    }

    /** Reject: soft delete. */
    public void markRejected(ObjectId commentId) {
        collection.updateOne(eq("_id", commentId), set("isDeleted", true));
    }
}
