package com.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Quiz")
public class Quiz {

    @Id
    private String id;
    private String courseId;
    private String title;
    private int passingScore;
    private int timeLimit;
    private boolean shuffleQuestions;
    @Builder.Default
    private List<QuizQuestion> questions = new ArrayList<>();
}
