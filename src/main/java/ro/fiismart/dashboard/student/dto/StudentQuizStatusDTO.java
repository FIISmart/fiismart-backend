package ro.fiismart.dashboard.student.dto;

import lombok.Data;

@Data
public class StudentQuizStatusDTO {
    private boolean hasQuiz;
    private String quizId;
    private String status;
    private Integer latestScore;
}
