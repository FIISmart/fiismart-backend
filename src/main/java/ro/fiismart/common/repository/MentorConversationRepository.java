package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.common.model.MentorConversation;

import java.util.Optional;

public interface MentorConversationRepository extends MongoRepository<MentorConversation, String> {
    Optional<MentorConversation> findByRequestId(String requestId);
    Optional<MentorConversation> findByIdAndStudentIdOrIdAndTutorId(String id, String studentId, String sameId, String tutorId);
}
