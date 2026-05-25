package ro.fiismart.chat.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    /** Hard cap on stored messages per thread — see {@link #trim(ChatSession)}. */
    public static final int MAX_MESSAGES = 30;

    /** Default page size when the sidebar lists threads. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    private final ChatSessionRepository repository;

    public ChatService(ChatSessionRepository repository) {
        this.repository = repository;
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
     * Appends {@code message} to {@code session} (in-place), applies the
     * 30-message trim, bumps {@code updatedAt}, and persists. Returns the
     * saved entity so the streaming path can also expose the new message id.
     *
     * <p>NB: This method must be invoked with an entity loaded via
     * {@link #loadOwned} — it does not re-check ownership itself.
     */
    public ChatSession appendMessage(ChatSession session, ChatMessage message) {
        if (session.getMessages() == null) {
            session.setMessages(new ArrayList<>());
        }
        if (message.getTs() == null) {
            message.setTs(Instant.now());
        }
        session.getMessages().add(message);
        trim(session);
        session.setUpdatedAt(Instant.now());
        return repository.save(session);
    }

    public ChatSession save(ChatSession session) {
        session.setUpdatedAt(Instant.now());
        return repository.save(session);
    }

    /**
     * If a session has more than {@link #MAX_MESSAGES} entries, drop the
     * oldest user/assistant <em>pair</em> from the front. Tool messages are
     * kept adjacent to the assistant turn that produced them — we never
     * orphan a {@code tool} message by dropping its preceding assistant.
     *
     * <p>Strategy: find the first {@code user} message; advance until we
     * also pass the first following {@code assistant}; drop everything up
     * to and including that assistant (which sweeps any interleaved
     * {@code tool} entries). Repeat until back under the cap.
     */
    static void trim(ChatSession session) {
        List<ChatMessage> messages = session.getMessages();
        if (messages == null) {
            return;
        }
        while (messages.size() > MAX_MESSAGES) {
            int firstUserIdx = indexOfRole(messages, "user", 0);
            if (firstUserIdx < 0) {
                // No user message in the window — pathological, just drop
                // the oldest single entry to make progress.
                messages.remove(0);
                continue;
            }
            int firstAssistantAfter = indexOfRole(messages, "assistant", firstUserIdx + 1);
            int dropUpTo = (firstAssistantAfter < 0) ? firstUserIdx : firstAssistantAfter;
            // Remove [0 .. dropUpTo] inclusive. Removing from the front
            // repeatedly is O(n*n) — acceptable here because n is capped
            // at 30 and trim runs only on overflow.
            for (int i = 0; i <= dropUpTo; i++) {
                messages.remove(0);
            }
        }
    }

    private static int indexOfRole(List<ChatMessage> messages, String role, int from) {
        for (int i = from; i < messages.size(); i++) {
            if (role.equals(messages.get(i).getRole())) {
                return i;
            }
        }
        return -1;
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
