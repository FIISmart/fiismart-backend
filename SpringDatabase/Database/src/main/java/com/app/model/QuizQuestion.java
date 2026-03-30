package com.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestion {

    private String id;
    private String text;
    private String type;
    private int points;
    @Builder.Default
    private List<String> options = new ArrayList<>();
    private int correctIdx;
    private String explanation;
}
