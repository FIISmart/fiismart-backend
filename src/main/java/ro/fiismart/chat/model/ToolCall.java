package ro.fiismart.chat.model;

import java.util.Map;

/**
 * Embedded record of a single Gemini tool invocation produced during a chat
 * turn. Stored alongside the assistant {@link ChatMessage} that triggered it.
 *
 * <p>{@code name} matches the function-declaration name advertised to Gemini
 * (currently {@code createQuizDraft} or {@code createCourseDraft}). {@code
 * args} are the raw JSON arguments emitted by the model. {@code result} is
 * populated once {@code ChatToolHandler} has dispatched the call — kept as
 * {@link Object} so the same field handles both an {@code AiQuizDraftDTO} and
 * a {@code CourseDraftDTO} without forcing a sum type into Mongo.
 *
 * <p>Plain POJO (no Lombok) per the implementation plan — keeps the model
 * jar Lombok-free for downstream consumers that don't pull in annotations.
 */
public class ToolCall {

    private String name;
    private Map<String, Object> args;
    private Object result;

    public ToolCall() {
    }

    public ToolCall(String name, Map<String, Object> args, Object result) {
        this.name = name;
        this.args = args;
        this.result = result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
