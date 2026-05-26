package ro.fiismart.chat.dto;

import ro.fiismart.chat.dto.request.RouteContextDTO;
import ro.fiismart.chat.tools.ToolProgressEvent;

import java.util.function.Consumer;

/**
 * Everything a chat tool needs to dispatch safely: who's calling
 * ({@code userId}), where they're calling from ({@code routeContext} —
 * the active course / lecture / quiz), and where to stream progress
 * events back ({@code onProgress}).
 *
 * <p>Introduced when the dispatcher gained side-effecting tools
 * (buildFullCourse, addModule, ...): the new tools need the route to
 * pin a target courseId and the userId to enforce ownership. Passing
 * each value as a separate dispatch arg would have ballooned the
 * signature, so we collapsed them into this record.
 *
 * <p>{@code onProgress} is non-null by contract — callers that don't
 * want events pass {@code e -> {}}. The dispatcher does not check for
 * null and individual tool handlers assume {@code .accept(...)} is
 * always safe.
 */
public record ToolDispatchContext(
        String userId,
        RouteContextDTO routeContext,
        Consumer<ToolProgressEvent> onProgress
) {
}
