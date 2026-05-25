package ro.fiismart.chat.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.chat.dto.response.ChatSessionDTO;
import ro.fiismart.chat.dto.response.ChatSessionSummaryDTO;
import ro.fiismart.chat.model.ChatMessage;
import ro.fiismart.chat.model.ChatSession;
import ro.fiismart.chat.repository.ChatSessionRepository;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the CRUD surface of chat threads and the in-document message
 * trimming policy. Streaming / model-calling lives in
 * {@code GeminiChatService} so that this service stays cheap to call from
 * the controller-thread path (load/list/delete) while heavy AI work runs
 * on its own executor.
 *
 * <p>All ownership checks are performed by the repository helpers
 * ({@code findByIdAndUserId}) rather than load-then-compare on the
 * application side — this prevents an attacker who knows another user's
 * session id from observing even the existence of that thread.
 */
@Service
public class ChatService {

    /** Default for newly-created threads; replaced by the first user message. */
    public static final String DEFAULT_TITLE = "Conversatie noua";

    /** Hard cap on stored messages per thread — enforced atomically by
     *  {@link #appendMessage(ChatSession, ChatMessage)} via Mongo {@code $slice}. */
    public static final int MAX_MESSAGES = 30;

    /** Default page size when the sidebar lists threads. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    private final ChatSessionRepository repository;
    private final MongoTemplate mongoTemplate;

    public ChatService(ChatSessionRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public ChatSessionDTO createSession(String userId) {
        Instant now = Instant.now();
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(DEFAULT_TITLE);
        session.setMessages(new ArrayList<>());
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        return ChatSessionDTO.from(repository.save(session));
    }

    /**
     * Lists threads owned by {@code userId} most-recently-updated first.
     * Page size is bounded — even if a caller passes a huge {@code size},
     * we clamp to {@link #DEFAULT_PAGE_SIZE} max to keep the sidebar query
     * cheap. Page indices are 0-based.
     */
    public List<ChatSessionSummaryDTO> listSessions(String userId, int page, int size) {
        int clampedSize = (size <= 0 || size > DEFAULT_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;
        int clampedPage = Math.max(0, page);
        Page<ChatSession> result = repository.findByUserIdOrderByUpdatedAtDesc(
                userId, PageRequest.of(clampedPage, clampedSize));
        return result.map(ChatSessionSummaryDTO::from).getContent();
    }

    public ChatSessionDTO getSession(String sessionId, String userId) {
        return ChatSessionDTO.from(loadOwned(sessionId, userId));
    }

    /**
     * Loads the raw entity for callers that need to mutate it
     * (e.g. {@code GeminiChatService} appending the assistant's reply).
     * Centralized so the ownership check / not-found mapping is consistent.
     */
    public ChatSession loadOwned(String sessionId, String userId) {
        return repository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> sessionMissingOrForbidden(sessionId, userId));
    }

    public void deleteSession(String sessionId, String userId) {
        // Guard with findByIdAndUserId first so we can return 404/403
        // semantics instead of silently no-op'ing on a foreign id.
        loadOwned(sessionId, userId);
        repository.deleteByIdAndUserId(sessionId, userId);
    }

    public ChatSessionDTO renameSession(String sessionId, String userId, String newTitle) {
        ChatSession session = loadOwned(sessionId, userId);
        session.setTitle(newTitle);
        session.setUpdatedAt(Instant.now());
        return ChatSessionDTO.from(repository.save(session));
    }

    /**
     * Appends {@code message} to {@code session} atomically (Mongo
     * {@code $push} with a {@code $slice: -MAX_MESSAGES} trim), bumps
     * {@code updatedAt}, and returns the post-update entity.
     *
     * <p>Why atomic instead of load-mutate-save: two near-simultaneous turns
     * (user types fast + assistant streams) would otherwise race on the
     * same document — last-write-wins drops the other side's message.
     * The {@code $push} variant is a single round-trip and never has the
     * race; the {@code @Version} field on {@link ChatSession} catches any
     * future load-mutate-save call site that re-introduces the bug.</p>
     *
     * <p>The previous pair-aware trim is replaced by Mongo's
     * {@code $slice}, which simply keeps the last
     * {@link #MAX_MESSAGES} entries. We lose the "drop oldest user/assistant
     * pair as a unit" guarantee — in exchange the operation becomes
     * race-free and a single round-trip. Tool messages can in principle be
     * orphaned at the window edge; in practice they sit adjacent to the
     * assistant turn that produced them and roll off together.</p>
     *
     * <p>NB: This method must be invoked with an entity loaded via
     * {@link #loadOwned} — it does not re-check ownership itself.
     */
    public ChatSession appendMessage(ChatSession session, ChatMessage message) {
        if (message.getTs() == null) {
            message.setTs(Instant.now());
        }

        Instant now = Instant.now();
        Update update = new Update()
                .push("messages").slice(-MAX_MESSAGES).each(message)
                .set("updatedAt", now);

        // findAndModify with returnNew=true gives us the post-update document
        // in one round-trip — no need for a follow-up findById.
        ChatSession updated = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(session.getId())
                        .and("userId").is(session.getUserId())),
                update,
                FindAndModifyOptions.options().returnNew(true),
                ChatSession.class);

        if (updated == null) {
            // Session vanished between loadOwned and here — surface the same
            // not-found error callers already expect.
            throw new ResourceNotFoundException("ChatSession", session.getId());
        }
        return updated;
    }

    public ChatSession save(ChatSession session) {
        session.setUpdatedAt(Instant.now());
        return repository.save(session);
    }

    /**
     * Atomically set the title on a session. Used by the auto-title path
     * after the first assistant turn — issuing a full document save there
     * would race against any concurrent {@link #appendMessage} (and now
     * trip the {@code @Version} check). A focused {@code $set} is safer.
     */
    public void updateTitle(String sessionId, String userId, String newTitle) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(sessionId).and("userId").is(userId)),
                new Update().set("title", newTitle).set("updatedAt", Instant.now()),
                ChatSession.class);
    }

    /**
     * Mongo's repository helpers don't distinguish "row doesn't exist" from
     * "row exists but belongs to someone else". From a security standpoint
     * we want both paths to look identical to the client — surface
     * {@link ResourceNotFoundException} so an attacker can't probe for the
     * existence of foreign session ids.
     *
     * <p>Throws {@link ForbiddenException} only if the session does exist
     * but is owned by another user — currently unreachable because
     * {@code findByIdAndUserId} already filters, but kept for future
     * audit-trail hooks.
     */
    private RuntimeException sessionMissingOrForbidden(String sessionId, String userId) {
        return repository.findById(sessionId)
                .map(s -> userId.equals(s.getUserId())
                        ? (RuntimeException) new ResourceNotFoundException("ChatSession", sessionId)
                        : new ForbiddenException("Chat session " + sessionId + " is not accessible"))
                .orElseGet(() -> new ResourceNotFoundException("ChatSession", sessionId));
    }
}
