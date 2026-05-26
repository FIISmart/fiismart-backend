package ro.fiismart.ai.dto.response;

import java.util.List;

/**
 * Module-level quiz attached to a {@link GeneratedModule}. Nullable on
 * the parent when {@code includeQuizzes=false} was passed to the
 * generator.
 */
public record GeneratedQuiz(
        String title,
        Integer passingScore,
        List<GeneratedQuizQuestion> questions
) {
}
