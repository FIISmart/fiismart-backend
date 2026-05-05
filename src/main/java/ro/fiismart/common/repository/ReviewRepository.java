package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.common.model.Review;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByCourseIdAndDeletedFalse(String courseId);

    List<Review> findByStudentIdAndDeletedFalse(String studentId);

    java.util.Optional<Review> findByStudentIdAndCourseIdAndDeletedFalse(String studentId, String courseId);

    List<Review> findByCourseIdAndStarsAndDeletedFalse(String courseId, int stars);

    boolean existsByStudentIdAndCourseIdAndDeletedFalse(String studentId, String courseId);

    long countByCourseIdAndDeletedFalse(String courseId);

    void deleteByCourseId(String courseId);
}
