package ro.fiismart.chat.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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

    /**
     * Mongo optimistic-locking version. Spring Data increments this on every
     * {@code save} and throws {@link org.springframework.dao.OptimisticLockingFailureException}
     * if the in-memory copy is stale. We use {@code MongoTemplate.updateFirst}
     * with {@code $push} for the hot append-message path, which is naturally
     * atomic, but the field is also useful for any future load-mutate-save
     * call sites (and matches Spring Data's tracking expectations).
     */
    @Version
    private Long version;

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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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
