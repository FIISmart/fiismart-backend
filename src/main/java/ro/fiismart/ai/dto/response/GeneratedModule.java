package ro.fiismart.ai.dto.response;

import java.util.List;

/**
 * One module inside an AI-generated course tree.
 *
 * <p>{@code quiz} is nullable: it's only populated when the generator
 * call set {@code includeQuizzes=true}. The build orchestrator only
 * persists a {@code ModuleQuiz} when the field is non-null.
 */
public record GeneratedModule(
        String title,
        String description,
        List<GeneratedLecture> lectures,
        GeneratedQuiz quiz
) {
}
