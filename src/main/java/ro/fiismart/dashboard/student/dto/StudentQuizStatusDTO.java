package ro.fiismart.dashboard.student.dto;

import lombok.Data;

@Data
public class StudentQuizStatusDTO {
    private boolean hasQuiz;
    private String quizId;
    private String title;
    private String scope;
    private String moduleId;
    private String lectureId;
    private String status;
    private String statusLabel;
    private Integer latestScore;
    private Integer attemptCount;
    private Boolean passed;
}
