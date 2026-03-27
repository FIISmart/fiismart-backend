package org.example;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DatabaseSeeder seeder = new DatabaseSeeder("FIISmart");
        MongoDatabase db = seeder.getMongoDatabase();

        db.getCollection("Users").drop();
        db.getCollection("Courses").drop();
        db.getCollection("Quiz").drop();
        db.getCollection("Enrollments").drop();
        db.getCollection("Comments").drop();
        db.getCollection("Review").drop();
        db.getCollection("QuizAttempt").drop();

        List<ObjectId> teacherIds = seeder.seedTeachers(db);
        List<ObjectId> studentIds = seeder.seedStudents(db);
        seeder.seedAdmins(db);
        List<ObjectId> courseIds = seeder.seedCourses(db, teacherIds);
        seeder.seedQuizzes(db, courseIds);
        seeder.seedEnrollments(db, studentIds, courseIds);
        seeder.seedQuizAttempts(db, studentIds, courseIds);
        seeder.seedComments(db, studentIds, courseIds);
        seeder.seedReviews(db, studentIds, courseIds);

        System.out.println("Done!");
        seeder.close();
    }
}