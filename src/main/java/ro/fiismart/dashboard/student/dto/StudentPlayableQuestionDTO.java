package ro.fiismart.dashboard.student.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.ModuleQuizQuestion;
import ro.fiismart.common.model.QuizQuestion;

import java.util.List;

/**
 * Student-facing projection of a quiz question.
 *
 * <p>Deliberately omits {@code correctIdx}, {@code correctText}, and
 * {@code explanation} so a student cannot read the answer key from the
 * network response before submitting. The canonical answer fields stay
 * server-side and are used by {@code QuizAttemptService} to score
 * submissions.
 */
@Data
@Builder
public class StudentPlayableQuestionDTO {

    private String id;
    private String text;
    private String imageUrl;
    private String type;
    private int points;
    private List<String> options;

    public static StudentPlayableQuestionDTO fromModuleQuestion(ModuleQuizQuestion q) {
        if (q == null) return null;
        return StudentPlayableQuestionDTO.builder()
                .id(q.getId())
                .text(q.getText())
                .imageUrl(q.getImageUrl())
                .type(q.getType())
                .points(q.getPoints())
                .options(q.getOptions())
                .build();
    }

    public static StudentPlayableQuestionDTO fromLegacyQuestion(QuizQuestion q) {
        if (q == null) return null;
        return StudentPlayableQuestionDTO.builder()
                .id(q.getId())
                .text(q.getText())
                .imageUrl(null)
                .type(q.getType())
                .points(q.getPoints())
                .options(q.getOptions())
                .build();
    }
}
