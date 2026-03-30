package com.app.dto.response;

import com.app.model.Answer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptResponse {

    private String id;
    private String quizId;
    private String courseId;
    private String studentId;
    private Date attemptedAt;
    private int score;
    private boolean passed;
    private int timeTakenSecs;
    private List<Answer> answers;
}
