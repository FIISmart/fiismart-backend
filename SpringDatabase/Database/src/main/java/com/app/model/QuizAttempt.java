package com.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "QuizAttempt")
public class QuizAttempt {

    @Id
    private String id;
    private String quizId;
    private String courseId;
    private String studentId;
    private Date attemptedAt;
    private int score;
    private boolean passed;
    private int timeTakenSecs;
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();
}
