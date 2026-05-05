package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.common.model.Enrollment;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {

    List<Enrollment> findByStudentId(String studentId);

    List<Enrollment> findByCourseId(String courseId);

    List<Enrollment> findByStudentIdAndStatus(String studentId, String status);

    Optional<Enrollment> findByStudentIdAndCourseId(String studentId, String courseId);

    boolean existsByStudentIdAndCourseId(String studentId, String courseId);

    long countByCourseId(String courseId);

    long countByCourseIdAndStatus(String courseId, String status);

    void deleteByStudentIdAndCourseId(String studentId, String courseId);

    void deleteByCourseId(String courseId);
}
