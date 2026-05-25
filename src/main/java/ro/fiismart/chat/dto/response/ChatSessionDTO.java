package ro.fiismart.chat.dto.response;

import ro.fiismart.chat.model.ChatMessage;
import ro.fiismart.chat.model.ChatSession;

import java.time.Instant;
import java.util.List;

/**
 * Full chat-session payload — id + title + every message in the thread.
 *
 * <p>Deliberately omits {@code userId}: the only caller is the owning user
 * (enforced by {@link ro.fiismart.chat.repository.ChatSessionRepository
 * #findByIdAndUserId}) and leaking the Cognito sub back to the client adds
 * nothing.
 */
public record ChatSessionDTO(
        String id,
        String title,
        List<ChatMessage> messages,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChatSessionDTO from(ChatSession session) {
        return new ChatSessionDTO(
                session.getId(),
                session.getTitle(),
                session.getMessages(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
