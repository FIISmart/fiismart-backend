package ro.fiismart.quiz.dto.modulequiz;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.ModuleQuizQuestion;

import java.util.List;

@Data
@Builder
public class ModuleQuizQuestionResponse {

    private String id;
    private String text;
    private String imageUrl;
    private String type;
    private int points;
    private List<String> options;
    private Integer correctIdx;
    private String correctText;
    /** Reference answer for {@code "free_text"} questions (AI grader rubric). */
    private String sampleAnswer;
    /** Required concepts for {@code "free_text"} questions (AI grader rubric). */
    private List<String> keyConcepts;
    /** Pass threshold 0-100 for {@code "free_text"} questions. */
    private Integer passThreshold;
    private String explanation;

    public static ModuleQuizQuestionResponse fromModel(ModuleQuizQuestion q) {
        if (q == null) return null;
        return ModuleQuizQuestionResponse.builder()
                .id(q.getId())
                .text(q.getText())
                .imageUrl(q.getImageUrl())
                .type(q.getType())
                .points(q.getPoints())
                .options(q.getOptions())
                .correctIdx(q.getCorrectIdx())
                .correctText(q.getCorrectText())
                .sampleAnswer(q.getSampleAnswer())
                .keyConcepts(q.getKeyConcepts())
                .passThreshold(q.getPassThreshold())
                .explanation(q.getExplanation())
                .build();
    }
}
