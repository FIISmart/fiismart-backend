package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.common.model.Notification;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    long countByRecipientIdAndReadFalse(String recipientId);
}
