package ro.fiismart.chat.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.chat.model.ChatSession;

import java.util.Optional;

/**
 * Repository for {@link ChatSession}.
 *
 * <p>The {@code findByIdAndUserId} / {@code deleteByIdAndUserId} pair lets us
 * enforce ownership in a single round-trip — no need to load-then-check on
 * the application side, and a leaked id alone is not enough to read or
 * delete someone else's thread.
 */
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    Page<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);

    Optional<ChatSession> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);
}
