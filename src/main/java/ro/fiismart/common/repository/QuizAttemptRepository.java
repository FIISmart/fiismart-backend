package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import ro.fiismart.common.model.QuizAttempt;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends MongoRepository<QuizAttempt, String> {

    List<QuizAttempt> findByStudentId(String studentId);

    List<QuizAttempt> findByQuizId(String quizId);

    List<QuizAttempt> findByStudentIdAndQuizId(String studentId, String quizId);

    List<QuizAttempt> findByStudentIdAndCourseId(String studentId, String courseId);

    Optional<QuizAttempt> findFirstByStudentIdAndQuizIdAndStatus(String studentId, String quizId, String status);

    Optional<QuizAttempt> findTopByStudentIdAndQuizIdOrderByAttemptedAtDesc(String studentId, String quizId);

    long countByStudentIdAndQuizId(String studentId, String quizId);

    @Query("{ 'quizId': ?0, 'passed': true }")
    List<QuizAttempt> findPassedByQuizId(String quizId);

    List<QuizAttempt> findByQuizIdAndPassed(String quizId, boolean passed);

    boolean existsByStudentIdAndQuizIdAndPassed(String studentId, String quizId, boolean passed);

    long countByQuizIdAndPassed(String quizId, boolean passed);

    QuizAttempt findFirstByStudentIdAndQuizIdOrderByAttemptedAtDesc(String studentId, String quizId);

    void deleteByQuizId(String quizId);

    void deleteByCourseId(String courseId);

    void deleteByStudentId(String studentId);
}
