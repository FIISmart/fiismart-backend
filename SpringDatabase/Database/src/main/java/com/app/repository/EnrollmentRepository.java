package com.app.repository;

import com.app.model.Enrollment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {

    Optional<Enrollment> findByStudentIdAndCourseId(String studentId, String courseId);

    List<Enrollment> findByStudentId(String studentId);

    List<Enrollment> findByCourseId(String courseId);

    List<Enrollment> findByStudentIdAndStatus(String studentId, String status);

    boolean existsByStudentIdAndCourseId(String studentId, String courseId);

    long countByCourseId(String courseId);

    long countByCourseIdAndStatus(String courseId, String status);

    void deleteByStudentIdAndCourseId(String studentId, String courseId);

    void deleteByCourseId(String courseId);
}
