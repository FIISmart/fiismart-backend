package ro.fiismart.chat.model;

import java.time.Instant;
import java.util.List;

/**
 * Embedded message inside a {@link ChatSession}.
 *
 * <p>{@code role} is one of:
 * <ul>
 *   <li>{@code "user"} — text typed by the human</li>
 *   <li>{@code "assistant"} — the model's reply (may carry {@code toolCalls})</li>
 *   <li>{@code "tool"} — synthetic record of a tool invocation + result,
 *       persisted so subsequent turns can see the side-effect history</li>
 * </ul>
 *
 * <p>Kept as a plain POJO (no Lombok) to make the persisted shape obvious
 * and to avoid lock-in for callers reading messages back out.
 */
public class ChatMessage {

    private String role;
    private String content;
    private List<ToolCall> toolCalls;
    private Instant ts;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content, List<ToolCall> toolCalls, Instant ts) {
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls;
        this.ts = ts;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public Instant getTs() {
        return ts;
    }

    public void setTs(Instant ts) {
        this.ts = ts;
    }
}
