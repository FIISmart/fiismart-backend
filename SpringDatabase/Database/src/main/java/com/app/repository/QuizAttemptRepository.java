package com.app.repository;

import com.app.model.QuizAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizAttemptRepository extends MongoRepository<QuizAttempt, String> {

    List<QuizAttempt> findByStudentId(String studentId);

    List<QuizAttempt> findByQuizId(String quizId);

    List<QuizAttempt> findByStudentIdAndQuizId(String studentId, String quizId);

    List<QuizAttempt> findByStudentIdAndCourseId(String studentId, String courseId);

    QuizAttempt findFirstByStudentIdAndQuizIdOrderByAttemptedAtDesc(String studentId, String quizId);

    List<QuizAttempt> findByQuizIdAndPassed(String quizId, boolean passed);

    boolean existsByStudentIdAndQuizIdAndPassed(String studentId, String quizId, boolean passed);

    long countByStudentIdAndQuizId(String studentId, String quizId);

    long countByQuizIdAndPassed(String quizId, boolean passed);

    void deleteByQuizId(String quizId);

    void deleteByCourseId(String courseId);

    void deleteByStudentId(String studentId);
}
