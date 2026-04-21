package com.fiismart.backend.seeder;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import database.model.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class LocalSeeder {

    private static final Random random = new Random();
    private static final int NUM_TEACHERS = 5;
    private static final int NUM_STUDENTS = 5;
    private static final int NUM_ADMINS = 2;
    private static final int NUM_COURSES = 10;

    private final MongoClient client;
    private final MongoDatabase db;

    public LocalSeeder(String databaseName) {
        Dotenv dotenv = Dotenv.load();
        String connectionString = dotenv.get("MONGO_CONNECTION_STRING");
        if (connectionString == null) {
            throw new RuntimeException("MONGO_CONNECTION_STRING nu a fost găsită în .env");
        }
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();
        this.client = MongoClients.create(settings);
        this.db = client.getDatabase(databaseName);
        System.out.println("✓ Connected to database: " + databaseName);
    }

    public void close() {
        client.close();
    }

    // ── CLEANUP ──────────────────────────────────────────────────────────────

    public void dropAll() {
        db.getCollection("Users").drop();
        db.getCollection("Courses").drop();
        db.getCollection("Quiz").drop();
        db.getCollection("Enrollments").drop();
        db.getCollection("Comments").drop();
        db.getCollection("Review").drop();
        db.getCollection("QuizAttempt").drop();
        System.out.println("✓ Dropped all collections");
    }

    // ── USERS ────────────────────────────────────────────────────────────────

    public List<ObjectId> seedTeachers() {
        MongoCollection<Document> col = db.getCollection("Users");
        List<ObjectId> ids = new ArrayList<>();
        for (int i = 1; i <= NUM_TEACHERS; i++) {
            ObjectId id = new ObjectId();
            ids.add(id);
            Document doc = new Document()
                    .append("_id", id)
                    .append("displayName", "Teacher " + i)
                    .append("email", "teacher" + i + "@fiismart.local")
                    .append("banned", false)
                    .append("role", "teacher")
                    .append("createdAt", daysAgo(365))
                    .append("lastLoginAt", daysAgo(7))
                    .append("bannedBy", null)
                    .append("bannedAt", null)
                    .append("banReason", null)
                    .append("ownedCourses", new ArrayList<>())
                    .append("passwordHash", "plain_Secret");
            col.insertOne(doc);
        }
        System.out.println("✓ Seeded " + NUM_TEACHERS + " teachers");
        return ids;
    }

    public List<ObjectId> seedStudents() {
        MongoCollection<Document> col = db.getCollection("Users");
        List<ObjectId> ids = new ArrayList<>();
        for (int i = 1; i <= NUM_STUDENTS; i++) {
            ObjectId id = new ObjectId();
            ids.add(id);
            Document doc = new Document()
                    .append("_id", id)
                    .append("displayName", "Student " + i)
                    .append("email", "student" + i + "@fiismart.local")
                    .append("banned", false)
                    .append("role", "student")
                    .append("createdAt", daysAgo(365))
                    .append("lastLoginAt", daysAgo(7))
                    .append("bannedBy", null)
                    .append("bannedAt", null)
                    .append("banReason", null)
                    .append("sessions", new ArrayList<>())
                    .append("passwordHash", "plain_Secret")
                    .append("enrolledCourseIds", new ArrayList<>());
            col.insertOne(doc);
        }
        System.out.println("✓ Seeded " + NUM_STUDENTS + " students");
        return ids;
    }

    public void seedAdmins() {
        MongoCollection<Document> col = db.getCollection("Users");
        for (int i = 1; i <= NUM_ADMINS; i++) {
            Document doc = new Document()
                    .append("_id", new ObjectId())
                    .append("displayName", "Admin " + i)
                    .append("email", "admin" + i + "@fiismart.local")
                    .append("role", "admin")
                    .append("createdAt", daysAgo(365))
                    .append("lastLoginAt", daysAgo(7))
                    .append("passwordHash", "plain_Secret");
            col.insertOne(doc);
        }
        System.out.println("✓ Seeded " + NUM_ADMINS + " admins");
    }

    // ── COURSES WITH MODULES ─────────────────────────────────────────────────

    public List<ObjectId> seedCourses(List<ObjectId> teacherIds) {
        MongoCollection<Document> courseCol = db.getCollection("Courses");
        MongoCollection<Document> userCol = db.getCollection("Users");
        MongoCollection<Document> quizCol = db.getCollection("Quiz");
        List<ObjectId> courseIds = new ArrayList<>();

        for (int c = 1; c <= NUM_COURSES; c++) {
            ObjectId courseId = new ObjectId();
            courseIds.add(courseId);
            ObjectId teacherId = teacherIds.get(random.nextInt(teacherIds.size()));

            // 3-5 module per curs
            int moduleCount = 3 + random.nextInt(3);
            List<CourseModule> modules = new ArrayList<>();

            for (int m = 1; m <= moduleCount; m++) {
                ObjectId moduleId = new ObjectId();

                // 2-5 lecții per modul
                int lectureCount = 2 + random.nextInt(4);
                List<Lecture> lectures = new ArrayList<>();
                for (int l = 1; l <= lectureCount; l++) {
                    Lecture lecture = Lecture.builder()
                            .id(new ObjectId())
                            .moduleId(moduleId)
                            .title("Course " + c + " / Module " + m + " / Lecture " + l)
                            .videoUrl("https://example.com/video/" + c + "-" + m + "-" + l)
                            .imageUrls(new ArrayList<>())
                            .order(l)
                            .durationSecs(300 + random.nextInt(2700))
                            .publishedAt(daysAgo(180))
                            .build();
                    lectures.add(lecture);
                }

                // 50% dintre module au quiz
                ObjectId moduleQuizId = null;
                if (random.nextBoolean()) {
                    moduleQuizId = new ObjectId();
                    Quiz moduleQuiz = Quiz.builder()
                            .id(moduleQuizId)
                            .courseId(courseId)
                            .moduleId(moduleId)
                            .title("Quiz Module " + m)
                            .passingScore(70)
                            .timeLimit(15)
                            .shuffleQuestions(true)
                            .questions(generateQuestions(3 + random.nextInt(3)))
                            .build();
                    quizCol.insertOne(moduleQuiz.toDocument());
                }

                CourseModule module = CourseModule.builder()
                        .id(moduleId)
                        .title("Module " + m)
                        .description("Description for Module " + m + " of Course " + c)
                        .order(m)
                        .lectures(lectures)
                        .quizId(moduleQuizId)
                        .build();
                modules.add(module);
            }

            // 50% dintre cursuri au quiz final
            ObjectId courseQuizId = null;
            if (random.nextBoolean()) {
                courseQuizId = new ObjectId();
                Quiz courseQuiz = Quiz.builder()
                        .id(courseQuizId)
                        .courseId(courseId)
                        .moduleId(null)
                        .title("Final Quiz for Course " + c)
                        .passingScore(70)
                        .timeLimit(30)
                        .shuffleQuestions(true)
                        .questions(generateQuestions(5 + random.nextInt(5)))
                        .build();
                quizCol.insertOne(courseQuiz.toDocument());
            }

            Course course = Course.builder()
                    .id(courseId)
                    .title("Course " + c)
                    .description("Description for Course " + c)
                    .teacherId(teacherId)
                    .status("published")
                    .tags(List.of("tag-a", "tag-b"))
                    .thumbnailUrl("https://example.com/thumbnail/" + c + ".jpg")
                    .language("English")
                    .enrollmentCount(0)
                    .avgRating(0.0)
                    .modules(modules)
                    .isHidden(false)
                    .quizId(courseQuizId)
                    .createdAt(daysAgo(365))
                    .updatedAt(daysAgo(30))
                    .build();

            courseCol.insertOne(course.toDocument());

            userCol.updateOne(
                    new Document("_id", teacherId),
                    new Document("$push", new Document("ownedCourses", courseId)));
        }

        System.out.println("✓ Seeded " + NUM_COURSES + " courses with modules, lectures, and quizzes");
        return courseIds;
    }

    private List<QuizQuestion> generateQuestions(int count) {
        List<QuizQuestion> questions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            QuizQuestion q = QuizQuestion.builder()
                    .id(new ObjectId())
                    .text("Question " + i + ": Which option is correct?")
                    .type("multiple_choice")
                    .points(1 + random.nextInt(3))
                    .options(List.of("Option A", "Option B", "Option C", "Option D"))
                    .correctIdx(random.nextInt(4))
                    .explanation("Explanation for question " + i)
                    .build();
            questions.add(q);
        }
        return questions;
    }

    // ── ENROLLMENTS ──────────────────────────────────────────────────────────

    public void seedEnrollments(List<ObjectId> studentIds, List<ObjectId> courseIds) {
        MongoCollection<Document> enrollCol = db.getCollection("Enrollments");
        MongoCollection<Document> userCol = db.getCollection("Users");
        MongoCollection<Document> courseCol = db.getCollection("Courses");

        for (ObjectId studentId : studentIds) {
            int numCourses = 1 + random.nextInt(4);
            List<ObjectId> chosen = new ArrayList<>();
            while (chosen.size() < numCourses) {
                ObjectId cId = courseIds.get(random.nextInt(courseIds.size()));
                if (!chosen.contains(cId)) chosen.add(cId);
            }

            for (ObjectId courseId : chosen) {
                Document enrollment = new Document()
                        .append("_id", new ObjectId())
                        .append("studentId", studentId)
                        .append("courseId", courseId)
                        .append("enrolledAt", daysAgo(180))
                        .append("completedAt", null)
                        .append("status", "enrolled")
                        .append("lectureProgress", new ArrayList<>())
                        .append("lastAccessedAt", daysAgo(7))
                        .append("overallProgress", 0);
                enrollCol.insertOne(enrollment);

                userCol.updateOne(
                        new Document("_id", studentId),
                        new Document("$push", new Document("enrolledCourseIds", courseId)));

                courseCol.updateOne(
                        new Document("_id", courseId),
                        new Document("$inc", new Document("enrollmentCount", 1)));
            }
        }
        System.out.println("✓ Seeded enrollments");
    }

    // ── COMMENTS ─────────────────────────────────────────────────────────────

    public void seedComments(List<ObjectId> studentIds, List<ObjectId> courseIds) {
        MongoCollection<Document> commentCol = db.getCollection("Comments");
        MongoCollection<Document> courseCol = db.getCollection("Courses");
        MongoCollection<Document> userCol = db.getCollection("Users");

        for (ObjectId courseId : courseIds) {
            Document courseDoc = courseCol.find(new Document("_id", courseId)).first();
            if (courseDoc == null) continue;
            Course course = Course.fromDocument(courseDoc);
            if (course.getModules() == null || course.getModules().isEmpty()) continue;

            // găsim un student înscris la curs (caut direct prin Users)
            ObjectId enrolledStudentId = findEnrolledStudent(userCol, studentIds, courseId);
            if (enrolledStudentId == null) continue;

            for (CourseModule module : course.getModules()) {
                if (module.getLectures() == null) continue;
                for (Lecture lecture : module.getLectures()) {
                    int commentCount = 1 + random.nextInt(3);
                    for (int i = 0; i < commentCount; i++) {
                        Document comment = new Document()
                                .append("_id", new ObjectId())
                                .append("lectureId", lecture.getId())
                                .append("courseId", courseId)
                                .append("authorId", enrolledStudentId)
                                .append("body", "Sample comment on " + lecture.getTitle())
                                .append("createdAt", daysAgo(90))
                                .append("updatedAt", null)
                                .append("isDeleted", false)
                                .append("parentCommentId", null)
                                .append("likeCount", random.nextInt(20))
                                .append("likedBy", new ArrayList<>())
                                .append("moderationFlags", new ArrayList<>())
                                .append("flagCount", 0);
                        commentCol.insertOne(comment);
                    }
                }
            }
        }
        System.out.println("✓ Seeded comments");
    }

    private ObjectId findEnrolledStudent(MongoCollection<Document> userCol, List<ObjectId> studentIds, ObjectId courseId) {
        for (int i = 0; i < 50; i++) {
            ObjectId sId = studentIds.get(random.nextInt(studentIds.size()));
            Document s = userCol.find(new Document("_id", sId)).first();
            if (s == null) continue;
            List<ObjectId> enrolled = s.getList("enrolledCourseIds", ObjectId.class);
            if (enrolled != null && enrolled.contains(courseId)) {
                return sId;
            }
        }
        return null;
    }

    // ── REVIEWS ──────────────────────────────────────────────────────────────

    public void seedReviews(List<ObjectId> studentIds, List<ObjectId> courseIds) {
        MongoCollection<Document> reviewCol = db.getCollection("Review");
        MongoCollection<Document> courseCol = db.getCollection("Courses");
        MongoCollection<Document> userCol = db.getCollection("Users");

        for (ObjectId courseId : courseIds) {
            List<ObjectId> reviewedBy = new ArrayList<>();
            double totalStars = 0;

            for (ObjectId studentId : studentIds) {
                Document s = userCol.find(new Document("_id", studentId)).first();
                if (s == null) continue;
                List<ObjectId> enrolled = s.getList("enrolledCourseIds", ObjectId.class);
                if (enrolled == null || !enrolled.contains(courseId)) continue;
                if (random.nextBoolean()) continue;
                if (reviewedBy.contains(studentId)) continue;
                reviewedBy.add(studentId);

                int stars = 1 + random.nextInt(5);
                totalStars += stars;

                Document review = new Document()
                        .append("_id", new ObjectId())
                        .append("studentId", studentId)
                        .append("courseId", courseId)
                        .append("stars", stars)
                        .append("body", "Review from student for course " + courseId.toHexString())
                        .append("createdAt", daysAgo(60))
                        .append("isDeleted", false)
                        .append("deletedBy", null);
                reviewCol.insertOne(review);
            }

            if (!reviewedBy.isEmpty()) {
                double avg = Math.round((totalStars / reviewedBy.size()) * 10.0) / 10.0;
                courseCol.updateOne(
                        new Document("_id", courseId),
                        new Document("$set", new Document("avgRating", avg)));
            }
        }
        System.out.println("✓ Seeded reviews");
    }

    // ── QUIZ ATTEMPTS ────────────────────────────────────────────────────────

    public void seedQuizAttempts(List<ObjectId> studentIds, List<ObjectId> courseIds) {
        MongoCollection<Document> attemptCol = db.getCollection("QuizAttempt");
        MongoCollection<Document> quizCol = db.getCollection("Quiz");
        MongoCollection<Document> userCol = db.getCollection("Users");

        for (ObjectId courseId : courseIds) {
            // luăm toate quiz-urile care aparțin acestui curs (module + final)
            List<Document> quizDocs = new ArrayList<>();
            quizCol.find(new Document("courseId", courseId)).into(quizDocs);

            for (Document quizDoc : quizDocs) {
                ObjectId quizId = quizDoc.getObjectId("_id");

                for (ObjectId studentId : studentIds) {
                    Document s = userCol.find(new Document("_id", studentId)).first();
                    if (s == null) continue;
                    List<ObjectId> enrolled = s.getList("enrolledCourseIds", ObjectId.class);
                    if (enrolled == null || !enrolled.contains(courseId)) continue;

                    // 70% șansă să fi încercat quiz-ul
                    if (random.nextDouble() > 0.7) continue;

                    int score = random.nextInt(101);
                    boolean passed = score >= 70;

                    Document attempt = new Document()
                            .append("_id", new ObjectId())
                            .append("quizId", quizId)
                            .append("courseId", courseId)
                            .append("studentId", studentId)
                            .append("attemptedAt", daysAgo(60))
                            .append("score", score)
                            .append("passed", passed)
                            .append("timeTakenSecs", 60 + random.nextInt(1740))
                            .append("answers", new ArrayList<>());
                    attemptCol.insertOne(attempt);
                }
            }
        }
        System.out.println("✓ Seeded quiz attempts");
    }

    // ── UTIL ─────────────────────────────────────────────────────────────────

    private static Date daysAgo(int maxDays) {
        long millis = System.currentTimeMillis() - (long) random.nextInt(maxDays) * 24 * 3600 * 1000;
        return new Date(millis);
    }

    // ── MAIN ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        LocalSeeder seeder = new LocalSeeder("FIISmart");
        try {
            seeder.dropAll();
            List<ObjectId> teacherIds = seeder.seedTeachers();
            List<ObjectId> studentIds = seeder.seedStudents();
            seeder.seedAdmins();
            List<ObjectId> courseIds = seeder.seedCourses(teacherIds);
            seeder.seedEnrollments(studentIds, courseIds);
            seeder.seedComments(studentIds, courseIds);
            seeder.seedReviews(studentIds, courseIds);
            seeder.seedQuizAttempts(studentIds, courseIds);
            System.out.println("\n🎉 Seeding complete!");
        } finally {
            seeder.close();
        }
    }
}