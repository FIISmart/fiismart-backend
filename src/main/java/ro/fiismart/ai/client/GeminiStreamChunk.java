package ro.fiismart.ai.client;

import java.util.Map;

/**
 * Streaming chunk emitted by {@link GeminiClient#streamGenerate}.
 *
 * <p>Sealed so the consumer can pattern-match exhaustively:
 * <ul>
 *   <li>{@link TextDelta} — incremental text token(s) extracted from
 *       {@code candidates[0].content.parts[*].text}.</li>
 *   <li>{@link FunctionCall} — Gemini invoked a declared tool. Used by the
 *       chat package later in Phase 3 for createQuizDraft / createCourseDraft.</li>
 *   <li>{@link Done} — terminal chunk; carries {@code finishReason} (STOP,
 *       MAX_TOKENS, SAFETY, ...). Always the last chunk delivered.</li>
 * </ul>
 */
public sealed interface GeminiStreamChunk
        permits GeminiStreamChunk.TextDelta,
                GeminiStreamChunk.FunctionCall,
                GeminiStreamChunk.Done {

    record TextDelta(String text) implements GeminiStreamChunk {}

    record FunctionCall(String name, Map<String, Object> args) implements GeminiStreamChunk {}

    record Done(String finishReason) implements GeminiStreamChunk {}
}
