package ro.fiismart.chat.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.fiismart.chat.dto.request.RenameSessionRequest;
import ro.fiismart.chat.dto.response.ChatSessionDTO;
import ro.fiismart.chat.dto.response.ChatSessionSummaryDTO;
import ro.fiismart.chat.service.ChatService;

import java.util.List;

/**
 * REST surface for the chatbot's session metadata. The SSE message-stream
 * endpoint is added by Phase 3 in {@code ChatStreamController} — split so
 * the request/response cycle for plain CRUD doesn't share lifecycle with
 * the long-lived SSE connection.
 *
 * <p>{@code @PreAuthorize("isAuthenticated()")} at the class level mirrors
 * the global Cognito-Bearer pattern; per-resource ownership is enforced
 * by {@link ChatService} via {@code findByIdAndUserId}.
 */
@RestController
@RequestMapping("/api/v1/chat")
@PreAuthorize("isAuthenticated()")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDTO> createSession(@AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createSession(userId));
    }

    /**
     * Lists the caller's sessions, most-recent first. {@code page} / {@code
     * size} are bounded by {@link ChatService#DEFAULT_PAGE_SIZE} server-side
     * so a client can't blow up the query.
     */
    @GetMapping("/sessions")
    public List<ChatSessionSummaryDTO> listSessions(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return chatService.listSessions(userId, page, size);
    }

    @GetMapping("/sessions/{id}")
    public ChatSessionDTO getSession(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return chatService.getSession(id, userId);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        chatService.deleteSession(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/sessions/{id}")
    public ChatSessionDTO renameSession(
            @PathVariable String id,
            @Valid @RequestBody RenameSessionRequest body,
            @AuthenticationPrincipal String userId) {
        return chatService.renameSession(id, userId, body.title());
    }
}
