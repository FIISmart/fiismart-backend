package com.fiismart.backend.course.helper;

import com.mongodb.client.MongoCollection;
import database.dao.MongoConnectionPool;
import database.model.Comment;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Component
public class CommentQueryHelper {

    private final MongoCollection<Document> collection;

    public CommentQueryHelper() {
        this.collection = MongoConnectionPool.getInstance()
                .getDatabase()
                .getCollection("Comments");
    }

    public List<Comment> findByCourseId(ObjectId courseId) {
        List<Document> docs = new ArrayList<>();
        collection.find(and(eq("courseId", courseId), eq("isDeleted", false))).into(docs);
        return docs.stream().map(Comment::fromDocument).collect(Collectors.toList());
    }
}
