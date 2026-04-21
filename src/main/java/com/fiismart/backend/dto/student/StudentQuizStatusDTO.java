package com.fiismart.backend.dto.student;

import lombok.Data;

@Data
public class StudentQuizStatusDTO {
    private boolean hasQuiz;
    private String quizId;
    private String status;
    private Integer latestScore;
}