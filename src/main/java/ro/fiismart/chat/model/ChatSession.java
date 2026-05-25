package ro.fiismart.chat.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-user chat thread persisted in MongoDB collection {@code ChatSessions}.
 *
 * <p>Why a single document instead of one-row-per-message: messages are
 * always read together (the assistant needs the whole rolling window for
 * context) and the message volume per session is hard-capped at 30 by
 * {@link ro.fiismart.chat.service.ChatService#appendMessage}, so the per-doc
 * size stays bounded comfortably under Mongo's 16 MB limit.
 *
 * <p>{@code userId} is indexed because the sidebar query
 * {@code findByUserIdOrderByUpdatedAtDesc} runs on every chat-panel open.
 */
@Document(collection = "ChatSessions")
public class ChatSession {

    @Id
    private String id;

    /** Cognito {@code sub} of the owning user — never set client-side. */
    @Indexed
    private String userId;

    /** Defaults to "Conversatie noua"; auto-replaced after the first user turn. */
    private String title;

    private List<ChatMessage> messages = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    public ChatSession() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
