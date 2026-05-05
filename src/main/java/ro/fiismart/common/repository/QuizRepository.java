package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.common.model.Quiz;

import java.util.Optional;

public interface QuizRepository extends MongoRepository<Quiz, String> {

    Optional<Quiz> findByCourseId(String courseId);

    boolean existsByCourseId(String courseId);

    void deleteByCourseId(String courseId);
}
