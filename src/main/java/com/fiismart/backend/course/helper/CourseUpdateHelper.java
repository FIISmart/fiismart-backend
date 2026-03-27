package com.fiismart.backend.course.helper;

import com.mongodb.client.MongoCollection;
import database.dao.MongoConnectionPool;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

@Component
public class CourseUpdateHelper {

    private final MongoCollection<Document> collection;

    public CourseUpdateHelper() {
        this.collection = MongoConnectionPool.getInstance()
                .getDatabase()
                .getCollection("Courses");
    }

    public void updateTags(ObjectId courseId, List<String> tags) {
        collection.updateOne(eq("_id", courseId), set("tags", tags));
    }

    public void updateThumbnailUrl(ObjectId courseId, String thumbnailUrl) {
        collection.updateOne(eq("_id", courseId), set("thumbnailUrl", thumbnailUrl));
    }

    public void updateLanguage(ObjectId courseId, String language) {
        collection.updateOne(eq("_id", courseId), set("language", language));
    }
}
