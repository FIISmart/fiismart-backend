package ro.fiismart.chat.tools;

/**
 * One progress tick emitted by a chat-tool while it executes. Used by
 * the {@code CourseBuildOrchestrator} (multi-step build) and the
 * {@code CourseToolHandler} (single-action modify tools) to give the FE
 * a streaming checklist of what just happened.
 *
 * <ul>
 *   <li>{@code name} — tool name the event belongs to (so the FE can
 *       correlate the event back to its toolCall card).</li>
 *   <li>{@code step} / {@code total} — coarse progress counters; both
 *       may be {@code null} for one-shot modify tools where there's only
 *       a single event.</li>
 *   <li>{@code message} — short Romanian user-facing string ("Adaug
 *       modul: Introducere", "Quiz creat", "Eroare la pasul 4").</li>
 * </ul>
 *
 * <p>Plain record with no Lombok — runtime jar stays annotation-light.
 */
public record ToolProgressEvent(
        String name,
        Integer step,
        Integer total,
        String message
) {
}
