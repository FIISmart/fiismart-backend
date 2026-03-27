package org.example;

import com.github.javafaker.Faker;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

@Getter
@Setter
public class DatabaseSeeder {
    private final String connectionString;
    private final MongoDatabase mongoDatabase;
    private final String databaseName;
    private final MongoClient client;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    private String loadConnectionString() {
        Dotenv dotenv = Dotenv.load();

        String connString = dotenv.get("MONGO_CONNECTION_STRING");

        if (connString == null || connString.isEmpty()) {
            throw new RuntimeException("Variabila MONGO_CONNECTION_STRING nu a fost găsită în .env");
        }

        return connString;
    }

    public DatabaseSeeder(String databaseName) {
        this.databaseName = databaseName;
        this.connectionString = loadConnectionString();
        try {
            this.client = MongoClients.create(this.connectionString);
            this.mongoDatabase = client.getDatabase(this.databaseName);
            System.out.println("Succesfully connected to Mongo Database" + this.databaseName);
            client.getDatabase("admin").runCommand(new Document("ping", 1));
            System.out.println("✓ Connection verified");
        } catch (MongoException mongoException) {
            System.err.println("Failed to connect to MongoDB: " + mongoException.getMessage());
            throw new RuntimeException("Database connection failed", mongoException);
        }
    }

    public List<ObjectId> seedTeachers(MongoDatabase mongoDatabase) {
        MongoCollection<Document> column = mongoDatabase.getCollection("Users");
        List<ObjectId> teacherIds = new ArrayList<>();
        Faker faker = new Faker();
        for (int i = 0; i < 5; i++) {
            ObjectId id = new ObjectId();
            teacherIds.add(id);
            Document allFakeInfo = new Document()
                    .append("_id", id)
                    .append("displayName", faker.name().fullName())
                    .append("email", faker.internet().emailAddress())
                    .append("banned", false)
                    .append("role", "teacher")
                    .append("createdAt", faker.date().past(365, java.util.concurrent.TimeUnit.DAYS))
                    .append("lastLoginAt", faker.date().past(7, java.util.concurrent.TimeUnit.DAYS))
                    .append("bannedBy", null)
                    .append("bannedAt", null)
                    .append("banReason", null)
                    .append("ownedCourses", new ArrayList<>())
                    .append("passwordHash", passwordEncoder.encode("Secret"));


            column.insertOne(allFakeInfo);
        }
        return teacherIds;
    }

    public List<ObjectId> seedStudents(MongoDatabase mongoDatabase) {
        MongoCollection<Document> column = mongoDatabase.getCollection("Users");
        List<ObjectId> studentIds = new ArrayList<>();
        Faker faker = new Faker();
        for (int i = 0; i < 5; i++) {
            ObjectId id = new ObjectId();
            studentIds.add(id);
            Document allFakeInfo = new Document()
                    .append("_id", id)
                    .append("displayName", faker.name().fullName())
                    .append("email", faker.internet().emailAddress())
                    .append("banned", false)
                    .append("role", "student")
                    .append("createdAt", faker.date().past(365, java.util.concurrent.TimeUnit.DAYS))
                    .append("lastLoginAt", faker.date().past(7, java.util.concurrent.TimeUnit.DAYS))
                    .append("bannedBy", null)
                    .append("bannedAt", null)
                    .append("banReason", null)
                    .append("sessions", new ArrayList<>())
                    .append("passwordHash", passwordEncoder.encode("Secret"))
                    .append("enrolledCourseIds", new ArrayList<>());

            column.insertOne(allFakeInfo);
        }
        return studentIds;
    }

    public void seedAdmins(MongoDatabase mongoDatabase) {
        MongoCollection<Document> column = mongoDatabase.getCollection("Users");
        Faker faker = new Faker();
        for (int i = 0; i < 2; i++) {

            Document allFakeInfo = new Document()
                    .append("_id", new ObjectId())
                    .append("displayName", faker.name().fullName())
                    .append("email", faker.internet().emailAddress())
                    .append("role", "admin")
                    .append("createdAt", faker.date().past(365, java.util.concurrent.TimeUnit.DAYS))
                    .append("lastLoginAt", faker.date().past(7, java.util.concurrent.TimeUnit.DAYS))
                    .append("passwordHash", passwordEncoder.encode("Secret"));

            column.insertOne(allFakeInfo);
        }
    }

    public List<ObjectId> seedCourses(MongoDatabase mongoDatabase, List<ObjectId> teacherIds) {
        MongoCollection<Document> courseCollection = mongoDatabase.getCollection("Courses");
        MongoCollection<Document> userCollection = mongoDatabase.getCollection("Users");
        List<ObjectId> courseIds = new ArrayList<>();
        Faker faker = new Faker();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            ObjectId courseId = new ObjectId();
            courseIds.add(courseId);

            ObjectId teacherId = teacherIds.get(random.nextInt(teacherIds.size()));

            int lectureCount = random.nextInt(6) + 1;
            List<Document> lectures = new ArrayList<>();

            for (int j = 0; j < lectureCount; j++) {
                lectures.add(new Document()
                        .append("_id", new ObjectId())
                        .append("title", faker.educator().course())
                        .append("videoUrl", "https://youtube.com/watch?v=" + faker.regexify("[A-Za-z0-9]{11}"))
                        .append("imageUrls", new ArrayList<>())
                        .append("order", j + 1)
                        .append("durationSecs", random.nextInt(3000) + 300)
                        .append("publishedAt", faker.date().past(180, java.util.concurrent.TimeUnit.DAYS)));
            }

            Document course = new Document()
                    .append("_id", courseId)
                    .append("title", faker.educator().course())
                    .append("description", faker.lorem().paragraph())
                    .append("teacherId", teacherId)
                    .append("status", "published")
                    .append("tags", List.of(faker.hacker().noun(), faker.hacker().noun()))
                    .append("thumbnailUrl", faker.internet().image())
                    .append("language", "English")
                    .append("enrollmentCount", 0)
                    .append("avgRating", 0.0)
                    .append("lectures", lectures)
                    .append("isHidden", false)
                    .append("quizId", null)
                    .append("createdAt", faker.date().past(365, java.util.concurrent.TimeUnit.DAYS))
                    .append("updatedAt", faker.date().past(30, java.util.concurrent.TimeUnit.DAYS));

            courseCollection.insertOne(course);

            userCollection.updateOne(
                    new Document("_id", teacherId),
                    new Document("$push", new Document("ownedCourses", courseId)));
        }
        return courseIds;
    }

    public void seedQuizzes(MongoDatabase mongoDatabase, List<ObjectId> courseIds) {
        MongoCollection<Document> quizCollection = mongoDatabase.getCollection("Quiz");
        MongoCollection<Document> courseCollection = mongoDatabase.getCollection("Courses");
        Faker faker = new Faker();
        Random random = new Random();

        for (ObjectId courseId : courseIds) {
            ObjectId quizId = new ObjectId();

            int questionCount = random.nextInt(5) + 3;
            List<Document> questions = new ArrayList<>();

            for (int i = 0; i < questionCount; i++) {
                List<String> options = List.of(
                        faker.lorem().sentence(),
                        faker.lorem().sentence(),
                        faker.lorem().sentence(),
                        faker.lorem().sentence()
                );

                questions.add(new Document()
                        .append("_id", new ObjectId())
                        .append("text", "Filler question no faker for this :(")
                        .append("type", "multiple_choice")
                        .append("points", random.nextInt(3) + 1)
                        .append("options", options)
                        .append("correctIdx", random.nextInt(4))
                        .append("explanation", faker.lorem().sentence()));
            }

            Document quiz = new Document()
                    .append("_id", quizId)
                    .append("courseId", courseId)
                    .append("title", "Final Quiz")
                    .append("passingScore", 70)
                    .append("timeLimit", 30)
                    .append("shuffleQuestions", true)
                    .append("questions", questions);

            quizCollection.insertOne(quiz);

            courseCollection.updateOne(
                    new Document("_id", courseId),
                    new Document("$set", new Document("quizId", quizId)));
        }

    }

    public void close() {
        client.close();
    }

    public void seedEnrollments(MongoDatabase mongoDatabase, List<ObjectId> studentIds, List<ObjectId> courseIds) {
        MongoCollection<Document> enrollmentCollection = mongoDatabase.getCollection("Enrollments");
        MongoCollection<Document> userCollection = mongoDatabase.getCollection("Users");
        MongoCollection<Document> courseCollection = mongoDatabase.getCollection("Courses");
        Random random = new Random();
        Faker faker = new Faker();

        for (ObjectId studentId : studentIds) {
            int numCourses = random.nextInt(4) + 1;
            List<ObjectId> chosenCourses = new ArrayList<>();

            while (chosenCourses.size() < numCourses) {
                ObjectId courseId = courseIds.get(random.nextInt(courseIds.size()));
                if (!chosenCourses.contains(courseId)) {
                    chosenCourses.add(courseId);
                }
            }

            for (ObjectId courseId : chosenCourses) {
                Document enrollment = new Document()
                        .append("_id", new ObjectId())
                        .append("studentId", studentId)
                        .append("courseId", courseId)
                        .append("enrolledAt", faker.date().past(180, java.util.concurrent.TimeUnit.DAYS))
                        .append("completedAt", null)
                        .append("status", "enrolled")
                        .append("lectureProgress", new ArrayList<>())
                        .append("lastAccessedAt", faker.date().past(7, java.util.concurrent.TimeUnit.DAYS))
                        .append("overallProgress", 0);

                enrollmentCollection.insertOne(enrollment);


                userCollection.updateOne(
                        new Document("_id", studentId),
                        new Document("$push", new Document("enrolledCourseIds", courseId)));


                courseCollection.updateOne(
                        new Document("_id", courseId),
                        new Document("$inc", new Document("enrollmentCount", 1)));
            }
        }

    }

    public void seedComments(MongoDatabase mongoDatabase, List<ObjectId> studentIds, List<ObjectId> courseIds) {
        MongoCollection<Document> commentCollection = mongoDatabase.getCollection("Comments");
        MongoCollection<Document> userCollection = mongoDatabase.getCollection("Users");
        MongoCollection<Document> courseCollection = mongoDatabase.getCollection("Courses");
        Faker faker = new Faker();
        Random random = new Random();

        for (ObjectId courseId : courseIds) {

            Document course = courseCollection.find(new Document("_id", courseId)).first();
            if (course == null) continue;

            List<Document> lectures = course.getList("lectures", Document.class);
            if (lectures == null || lectures.isEmpty()) continue;

            for (Document lecture : lectures) {
                ObjectId lectureId = lecture.getObjectId("_id");
                int commentCount = random.nextInt(5) + 1;
                ObjectId appropriateStudId = null;
                boolean found = false;
                int attempts = 0;
                int maxAttempts = 100;

                while (!found && attempts < maxAttempts) {
                    attempts++;
                    ObjectId randomStudId = studentIds.get(random.nextInt(studentIds.size()));
                    Document student = userCollection.find(new Document("_id", randomStudId)).first();

                    if (student == null) {
                        continue;
                    }

                    List<ObjectId> enrolledCourseIds = student.getList("enrolledCourseIds", ObjectId.class);
                    if (enrolledCourseIds != null && enrolledCourseIds.contains(courseId)) {
                        appropriateStudId = randomStudId;
                        found = true;
                    }
                }


                if (!found) {
                    System.err.println("No enrolled student found for course: " + courseId);
                    continue;
                }

                for (int i = 0; i < commentCount; i++) {
                    Document comment = new Document()
                            .append("_id", new ObjectId())
                            .append("lectureId", lectureId)
                            .append("courseId", courseId)
                            .append("authorId", appropriateStudId)
                            .append("body", faker.lorem().paragraph())
                            .append("createdAt", faker.date().past(90, java.util.concurrent.TimeUnit.DAYS))
                            .append("updatedAt", null)
                            .append("isDeleted", false)
                            .append("parentCommentId", null)
                            .append("likeCount", random.nextInt(20))
                            .append("likedBy", new ArrayList<>())
                            .append("moderationFlags", new ArrayList<>())
                            .append("flagCount", 0);

                    commentCollection.insertOne(comment);
                }
            }
        }
    }

    public void seedReviews(MongoDatabase mongoDatabase, List<ObjectId> studentIds, List<ObjectId> courseIds) {
        MongoCollection<Document> reviewCollection = mongoDatabase.getCollection("Review");
        MongoCollection<Document> courseCollection = mongoDatabase.getCollection("Courses");
        MongoCollection<Document> userCollection = mongoDatabase.getCollection("Users");
        Faker faker = new Faker();
        Random random = new Random();

        for (ObjectId courseId : courseIds) {
            List<ObjectId> reviewedBy = new ArrayList<>();
            double totalStars = 0;

            for (ObjectId studentId : studentIds) {

                Document student = userCollection.find(new Document("_id", studentId)).first();
                if (student == null) continue;

                List<ObjectId> enrolledCourseIds = student.getList("enrolledCourseIds", ObjectId.class);
                if (enrolledCourseIds == null || !enrolledCourseIds.contains(courseId)) continue;

                if (random.nextBoolean()) continue;

                if (reviewedBy.contains(studentId)) continue;
                reviewedBy.add(studentId);

                int stars = random.nextInt(5) + 1;
                totalStars += stars;

                Document review = new Document()
                        .append("_id", new ObjectId())
                        .append("studentId", studentId)
                        .append("courseId", courseId)
                        .append("stars", stars)
                        .append("body", faker.lorem().paragraph())
                        .append("createdAt", faker.date().past(60, java.util.concurrent.TimeUnit.DAYS))
                        .append("isDeleted", false)
                        .append("deletedBy", null);

                reviewCollection.insertOne(review);
            }
            if (!reviewedBy.isEmpty()) {
                double avg = Math.round((totalStars / reviewedBy.size()) * 10.0) / 10.0;
                courseCollection.updateOne(
                        new Document("_id", courseId),
                        new Document("$set", new Document("avgRating", avg)));
            }
        }
    }

    public void seedQuizAttempts(MongoDatabase mongoDatabase, List<ObjectId> studentIds, List<ObjectId> courseIds) {
        MongoCollection<Document> attemptCollection = mongoDatabase.getCollection("QuizAttempt");
        MongoCollection<Document> courseCollection = mongoDatabase.getCollection("Courses");
        MongoCollection<Document> userCollection = mongoDatabase.getCollection("Users");
        Faker faker = new Faker();
        Random random = new Random();

        for (ObjectId courseId : courseIds) {

            Document course = courseCollection.find(new Document("_id", courseId)).first();
            if (course == null) continue;

            ObjectId quizId = course.getObjectId("quizId");
            if (quizId == null) continue;

            for (ObjectId studentId : studentIds) {

                Document student = userCollection.find(new Document("_id", studentId)).first();
                if (student == null) continue;

                List<ObjectId> enrolledCourseIds = student.getList("enrolledCourseIds", ObjectId.class);
                if (enrolledCourseIds == null || !enrolledCourseIds.contains(courseId)) continue;
                int score = random.nextInt(101);
                boolean passed = score >= 70;

                Document attempt = new Document()
                        .append("_id", new ObjectId())
                        .append("quizId", quizId)
                        .append("courseId", courseId)
                        .append("studentId", studentId)
                        .append("attemptedAt", faker.date().past(60, java.util.concurrent.TimeUnit.DAYS))
                        .append("score", score)
                        .append("passed", passed)
                        .append("timeTakenSecs", random.nextInt(1800) + 60)
                        .append("answers", new ArrayList<>());

                attemptCollection.insertOne(attempt);

            }
        }
    }
}