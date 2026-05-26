package ro.fiismart.ai.dto.response;

import java.util.List;

/**
 * One multiple-choice question inside a {@link GeneratedQuiz}.
 *
 * <p>v1 generates only {@code multiple_choice} questions — the schema
 * description should reinforce this to keep Gemini from emitting
 * free-text drafts which the build orchestrator does not persist.
 */
public record GeneratedQuizQuestion(
        String text,
        String type,
        List<String> options,
        Integer correctIdx,
        String explanation
) {
}
