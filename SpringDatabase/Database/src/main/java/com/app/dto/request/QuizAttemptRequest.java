package com.app.dto.request;

import com.app.model.Answer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class QuizAttemptRequest {

    @NotBlank
    private String quizId;

    @NotBlank
    private String courseId;

    @NotBlank
    private String studentId;

    @Min(0)
    private int score;

    private boolean passed;

    @Min(0)
    private int timeTakenSecs;

    @Builder.Default
    private List<Answer> answers = new ArrayList<>();
}
