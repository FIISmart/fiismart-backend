package ro.fiismart.quizattempt.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.Answer;

import java.util.Date;
import java.util.List;

@Data
@Builder
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
