package ro.fiismart.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/chat/sessions/{id}/messages/stream}.
 *
 * <p>{@code content} is the user-typed message (bounded so a runaway
 * client can't blow up the prompt). {@code routeContext} is optional —
 * when present, gets folded into the system prompt by
 * {@code ChatContextBuilder}.
 */
public record SendMessageRequest(
        @NotBlank @Size(max = 4000) String content,
        RouteContextDTO routeContext
) {
}
