package com.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionResponse {

    private String id;
    private String text;
    private String type;
    private int points;
    private List<String> options;
    private int correctIdx;
    private String explanation;
}
