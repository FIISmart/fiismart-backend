package com.fiismart.backend.course.dto.response;

import database.model.QuizQuestion;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizQuestionResponse {

    private String id;
    private String text;
    private String type;
    private int points;
    private List<String> options;
    private int correctIdx;
    private String correctText;
    private String explanation;

    public static QuizQuestionResponse fromModel(QuizQuestion q) {
        if (q == null) return null;
        return QuizQuestionResponse.builder()
                .id(q.getId() != null ? q.getId().toHexString() : null)
                .text(q.getText())
                .type(q.getType())
                .points(q.getPoints())
                .options(q.getOptions())
                .correctIdx(q.getCorrectIdx())
                .correctText(q.getCorrectText())
                .explanation(q.getExplanation())
                .build();
    }
}
