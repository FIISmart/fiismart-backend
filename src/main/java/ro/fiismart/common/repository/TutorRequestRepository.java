package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.common.model.TutorRequest;

import java.util.List;

public interface TutorRequestRepository extends MongoRepository<TutorRequest, String> {
    List<TutorRequest> findByStudentId(String studentId);
    List<TutorRequest> findByTutorId(String tutorId);
    boolean existsByStudentIdAndTutorIdAndStatus(String studentId, String tutorId, String status);
}
