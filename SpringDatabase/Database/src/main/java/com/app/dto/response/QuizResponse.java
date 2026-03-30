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
public class QuizResponse {

    private String id;
    private String courseId;
    private String title;
    private int passingScore;
    private int timeLimit;
    private boolean shuffleQuestions;
    private List<QuizQuestionResponse> questions;
}
