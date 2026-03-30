package com.app.repository;

import com.app.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends MongoRepository<Review, String> {

    Optional<Review> findByStudentIdAndCourseIdAndDeletedFalse(String studentId, String courseId);

    List<Review> findByCourseIdAndDeletedFalse(String courseId);

    List<Review> findByStudentIdAndDeletedFalse(String studentId);

    List<Review> findByCourseIdAndStarsAndDeletedFalse(String courseId, int stars);

    boolean existsByStudentIdAndCourseIdAndDeletedFalse(String studentId, String courseId);

    long countByCourseIdAndDeletedFalse(String courseId);

    void deleteByCourseId(String courseId);
}
