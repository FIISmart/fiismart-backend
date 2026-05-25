package ro.fiismart.chat.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ro.fiismart.chat.dto.request.SendMessageRequest;
import ro.fiismart.chat.model.ChatMessage;
import ro.fiismart.chat.model.ChatSession;
import ro.fiismart.chat.service.ChatService;
import ro.fiismart.chat.service.GeminiChatService;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSE message-streaming endpoint, kept separate from {@link ChatController}
 * so the long-lived emitter doesn't share dispatch concerns with plain
 * CRUD. Same {@code /api/v1/chat} base — only the suffix differs.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Verify ownership via {@code ChatService.loadOwned}.</li>
 *   <li>Append the user message synchronously (so a refresh mid-stream
 *       still shows the prompt).</li>
 *   <li>Hand off to {@link GeminiChatService#streamResponse} on a worker
 *       thread; return the emitter immediately so the Tomcat thread is
 *       released.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/chat")
@PreAuthorize("isAuthenticated()")
public class ChatStreamController {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamController.class);

    private final ChatService chatService;
    private final GeminiChatService geminiChatService;

    public ChatStreamController(ChatService chatService, GeminiChatService geminiChatService) {
        this.chatService = chatService;
        this.geminiChatService = geminiChatService;
    }

    @PostMapping(
            value = "/sessions/{id}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest body,
            @AuthenticationPrincipal String userId) {

        log.info("Chat stream: user={} session={} contentLen={}",
                userId, id, body.content() == null ? 0 : body.content().length());

        SseEmitter emitter = new SseEmitter(GeminiChatService.SSE_TIMEOUT_MS);

        // Ownership + append happen synchronously so any 403/404 surfaces
        // as a normal HTTP error response BEFORE the emitter is returned.
        ChatSession session;
        try {
            session = chatService.loadOwned(id, userId);
            ChatMessage userMsg = new ChatMessage(
                    "user",
                    body.content(),
                    null,
                    Instant.now()
            );
            session = chatService.appendMessage(session, userMsg);
        } catch (RuntimeException e) {
            // Let the GlobalExceptionHandler map ForbiddenException /
            // ResourceNotFoundException — we haven't yet started emitting.
            throw e;
        }

        ChatSession finalSession = session;

        // Per-request single-thread executor — Gemini streaming blocks on
        // socket reads; can't tie up a Tomcat worker for minutes.
        ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "chat-sse-stream-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        emitter.onCompletion(worker::shutdownNow);
        emitter.onTimeout(worker::shutdownNow);
        emitter.onError(t -> worker.shutdownNow());

        worker.submit(() -> {
            try {
                geminiChatService.streamResponse(finalSession, body.routeContext(), userId, emitter);
            } catch (Exception e) {
                // Don't leak exception text to the client — keep the
                // payload generic and surface a correlation id so we
                // can join the FE bug report to the server log line.
                String corrId = UUID.randomUUID().toString();
                log.warn("Chat stream worker failed session={} corrId={}",
                        finalSession.getId(), corrId, e);
                try {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("message", "Serviciul AI este temporar indisponibil.");
                    payload.put("correlationId", corrId);
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(payload, MediaType.APPLICATION_JSON));
                } catch (IOException ignored) {
                    // best effort
                }
                emitter.complete();
            }
        });

        return emitter;
    }
}
