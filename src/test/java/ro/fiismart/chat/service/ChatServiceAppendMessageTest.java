package ro.fiismart.chat.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import ro.fiismart.chat.model.ChatMessage;
import ro.fiismart.chat.model.ChatSession;
import ro.fiismart.chat.repository.ChatSessionRepository;
import ro.fiismart.common.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChatService#appendMessage} after it switched from
 * load-mutate-save to an atomic Mongo {@code $push} update.
 *
 * <p>The behaviour we lock in:
 * <ol>
 *   <li><b>Atomic dispatch:</b> the call must go through
 *       {@code MongoTemplate.findAndModify} with a {@code $push} update
 *       carrying the new message — NOT a full document {@code save}. This
 *       is what eliminates the concurrent-append race.</li>
 *   <li><b>Owner-scoped query:</b> the filter must restrict by both
 *       {@code _id} AND {@code userId} so a leaked session id alone
 *       can't be used to write into another user's thread.</li>
 *   <li><b>Trim-by-slice:</b> the update must apply
 *       {@code $slice: -MAX_MESSAGES} so Mongo keeps only the last
 *       30 entries regardless of role.</li>
 *   <li><b>404 surface on vanished session:</b> if {@code findAndModify}
 *       returns null (race with delete, or wrong owner), callers see
 *       the same {@link ResourceNotFoundException} they already expect
 *       from {@code loadOwned}.</li>
 * </ol>
 */
class ChatServiceAppendMessageTest {

    private static final String SESSION_ID = "session-123";
    private static final String USER_ID = "user-abc";

    @Test
    void appendMessage_usesFindAndModifyWithPushSliceUpdatedAt_andReturnsServerCopy() {
        ChatSessionRepository repo = mock(ChatSessionRepository.class);
        MongoTemplate mongo = mock(MongoTemplate.class);
        ChatService service = new ChatService(repo, mongo);

        // The post-update document we want findAndModify to return.
        ChatSession returned = new ChatSession();
        returned.setId(SESSION_ID);
        returned.setUserId(USER_ID);
        returned.setTitle("Conversatie noua");
        returned.setMessages(List.of(new ChatMessage("user", "buna", null, Instant.now())));
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(ChatSession.class)))
                .thenReturn(returned);

        ChatSession input = new ChatSession();
        input.setId(SESSION_ID);
        input.setUserId(USER_ID);

        ChatMessage msg = new ChatMessage("user", "buna", null, null);
        ChatSession result = service.appendMessage(input, msg);

        // 1. Caller never sees the repository.save path — only findAndModify.
        ArgumentCaptor<Query> queryCap = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCap = ArgumentCaptor.forClass(Update.class);
        ArgumentCaptor<FindAndModifyOptions> optsCap = ArgumentCaptor.forClass(FindAndModifyOptions.class);
        verify(mongo).findAndModify(queryCap.capture(), updateCap.capture(), optsCap.capture(), eq(ChatSession.class));

        // 2. Owner-scoped query: _id AND userId both pinned.
        String queryJson = queryCap.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains("\"_id\"").contains(SESSION_ID);
        assertThat(queryJson).contains("\"userId\"").contains(USER_ID);

        // 3. Update document carries $push with $slice (-30) and a $set on updatedAt.
        String updateJson = updateCap.getValue().getUpdateObject().toJson();
        assertThat(updateJson).contains("$push");
        assertThat(updateJson).contains("messages");
        assertThat(updateJson).contains("$slice");
        assertThat(updateJson).contains("-30");  // = -MAX_MESSAGES
        assertThat(updateJson).contains("$set");
        assertThat(updateJson).contains("updatedAt");

        // 4. returnNew(true) so the caller gets the post-update document.
        assertThat(optsCap.getValue().isReturnNew()).isTrue();

        // 5. The message has its timestamp populated even if caller passed null.
        assertThat(msg.getTs()).isNotNull();

        // 6. Returned session is the one Mongo handed back, not the input copy.
        assertThat(result).isSameAs(returned);
    }

    @Test
    void appendMessage_doesNotMutateClientTimestampWhenProvided() {
        ChatSessionRepository repo = mock(ChatSessionRepository.class);
        MongoTemplate mongo = mock(MongoTemplate.class);
        ChatService service = new ChatService(repo, mongo);

        ChatSession out = new ChatSession();
        out.setId(SESSION_ID);
        out.setUserId(USER_ID);
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(ChatSession.class)))
                .thenReturn(out);

        Instant explicit = Instant.parse("2025-01-01T10:00:00Z");
        ChatMessage msg = new ChatMessage("user", "salut", null, explicit);

        ChatSession input = new ChatSession();
        input.setId(SESSION_ID);
        input.setUserId(USER_ID);
        service.appendMessage(input, msg);

        // The service must not overwrite an explicitly-provided ts —
        // legacy migration code that backfills historical messages
        // relies on this contract.
        assertThat(msg.getTs()).isEqualTo(explicit);
    }

    @Test
    void appendMessage_returnsNullFromMongo_surfacesAsResourceNotFound() {
        ChatSessionRepository repo = mock(ChatSessionRepository.class);
        MongoTemplate mongo = mock(MongoTemplate.class);
        ChatService service = new ChatService(repo, mongo);

        // findAndModify returns null when no document matches the filter
        // — that means the session was deleted (or never owned by this
        // user) between loadOwned() and the push.
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(ChatSession.class)))
                .thenReturn(null);

        ChatSession input = new ChatSession();
        input.setId(SESSION_ID);
        input.setUserId(USER_ID);

        assertThatThrownBy(() -> service.appendMessage(input, new ChatMessage("user", "x", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
