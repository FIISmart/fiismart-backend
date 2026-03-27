package org.example;

import org.bson.types.ObjectId;
import database.dao.*;
import database.model.*;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("Initializare FIISmart Database Seeder...");

        try {
            UserDAO userDAO = new UserDAO();
            QuizDAO quizDAO = new QuizDAO();
            ReviewDAO reviewDAO = new ReviewDAO();
            QuizAttemptDAO attemptDAO = new QuizAttemptDAO();

            System.out.println("Curatare baza de date existenta...");

            String hashedPass = BCrypt.hashpw("parola123", BCrypt.gensalt());
            User teacher = User.builder()
                    .displayName("Prof. Dragos Burileanu")
                    .email("dragos@info.uaic.ro")
                    .role("teacher")
                    .passwordHash(hashedPass)
                    .createdAt(new Date())
                    .banned(false)
                    .build();

            ObjectId teacherId = userDAO.insert(teacher);
            System.out.println("Profesor creat: " + teacher.getEmail());

            ObjectId courseId = new ObjectId();
            userDAO.addOwnedCourse(teacherId, courseId);

            QuizQuestion q1 = QuizQuestion.builder()
                    .text("Ce face cuvantul 'static' in Java?")
                    .type("multiple_choice")
                    .points(5)
                    .options(Arrays.asList("Apartine clasei", "Apartine instantei", "E keyword de securitate"))
                    .correctIdx(0)
                    .explanation("Membrii statici apartin clasei, nu obiectelor individuale.")
                    .build();

            Quiz quiz = Quiz.builder()
                    .courseId(courseId)
                    .title("Test Grila Java Basics")
                    .passingScore(5)
                    .timeLimit(30)
                    .questions(Arrays.asList(q1))
                    .build();

            ObjectId quizId = quizDAO.insert(quiz);
            System.out.println("Quiz creat pentru cursul: " + courseId);

            User student = User.builder()
                    .displayName("Ionut Popescu")
                    .email("ionut.popescu@students.info.uaic.ro")
                    .role("student")
                    .passwordHash(BCrypt.hashpw("student123", BCrypt.gensalt()))
                    .createdAt(new Date())
                    .enrolledCourseIds(Arrays.asList(courseId))
                    .build();

            ObjectId studentId = userDAO.insert(student);
            System.out.println("Student creat: " + student.getEmail());

            Review review = Review.builder()
                    .studentId(studentId)
                    .courseId(courseId)
                    .stars(5)
                    .body("Un curs excelent, am invatat multe despre MongoDB!")
                    .createdAt(new Date())
                    .isDeleted(false)
                    .build();

            reviewDAO.insert(review);
            System.out.println("Review adaugat de student.");

            QuizAttempt attempt = QuizAttempt.builder()
                    .quizId(quizId)
                    .studentId(studentId)
                    .courseId(courseId)
                    .score(10)
                    .passed(true)
                    .attemptedAt(new Date())
                    .build();

            attemptDAO.insert(attempt);
            System.out.println("Attempt salvat. Scor: " + attempt.getScore());

            System.out.println("\n[STATISTICI DATABASE]");
            System.out.println("Total Studenti: " + userDAO.countByRole("student"));
            System.out.println("Media Rating Curs: " + reviewDAO.computeAvgRating(courseId));
            System.out.println("Rata de promovare Quiz: " + attemptDAO.countPassedByQuiz(quizId));
            System.out.println("---------------------------\n");

            System.out.println("Seeding finalizat cu succes.");

        } catch (Exception e) {
            System.err.println("Eroare la seeding: " + e.getMessage());
            e.printStackTrace();
        }
    }
}