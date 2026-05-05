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
    private int correctIdx;
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
                .explanation(q.getExplanation())
                .build();
    }
}
