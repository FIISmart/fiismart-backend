package ro.fiismart.quiz.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.Quiz;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class QuizResponse {

    private String id;
    private String courseId;
    private String title;
    private int passingScore;
    private int timeLimit;
    private boolean shuffleQuestions;
    private List<QuizQuestionResponse> questions;

    public static QuizResponse fromModel(Quiz quiz) {
        if (quiz == null) return null;
        return QuizResponse.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
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
