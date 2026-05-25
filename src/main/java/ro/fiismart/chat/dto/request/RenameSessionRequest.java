package ro.fiismart.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PATCH /api/v1/chat/sessions/{id}} — rename the thread.
 *
 * <p>Title length is capped so the sidebar doesn't have to truncate
 * defensively at render time.
 */
public record RenameSessionRequest(
        @NotBlank @Size(max = 120) String title
) {
}
