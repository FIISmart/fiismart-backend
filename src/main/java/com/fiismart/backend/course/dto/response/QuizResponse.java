package com.fiismart.backend.course.dto.response;

import database.model.Quiz;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class QuizResponse {

    private String id;
    private String courseId;
    private String moduleId;   // null for legacy course-wide quizzes
    private String title;
    private int passingScore;
    private int timeLimit;
    private boolean shuffleQuestions;
    private List<QuizQuestionResponse> questions;

    public static QuizResponse fromModel(Quiz quiz) {
        if (quiz == null) return null;
        return QuizResponse.builder()
                .id(quiz.getId() != null ? quiz.getId().toHexString() : null)
                .courseId(quiz.getCourseId() != null ? quiz.getCourseId().toHexString() : null)
                .moduleId(quiz.getModuleId() != null ? quiz.getModuleId().toHexString() : null)
                .title(quiz.getTitle())
                .passingScore(quiz.getPassingScore())
                .timeLimit(quiz.getTimeLimit())
                .shuffleQuestions(quiz.isShuffleQuestions())
                .questions(quiz.getQuestions() != null
                        ? quiz.getQuestions().stream().map(QuizQuestionResponse::fromModel).collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
