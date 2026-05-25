package ro.fiismart.chat.dto.response;

import ro.fiismart.chat.model.ChatSession;

import java.time.Instant;

/**
 * Lightweight sidebar row — id + title + last-update time, no message body.
 *
 * <p>{@code GET /api/v1/chat/sessions} returns a page of these; the full
 * thread is fetched lazily when the user opens a specific conversation.
 */
public record ChatSessionSummaryDTO(
        String id,
        String title,
        Instant updatedAt
) {

    public static ChatSessionSummaryDTO from(ChatSession session) {
        return new ChatSessionSummaryDTO(
                session.getId(),
                session.getTitle(),
                session.getUpdatedAt()
        );
    }
}
