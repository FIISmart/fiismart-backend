package ro.fiismart.quiz.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.ModuleQuiz;
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

    public static QuizResponse fromModuleQuiz(ModuleQuiz quiz) {
        if (quiz == null) return null;
        return QuizResponse.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
                .title(quiz.getTitle())
                .passingScore(quiz.getPassingScore())
                .timeLimit(quiz.getTimeLimit())
                .shuffleQuestions(quiz.isShuffleQuestions())
                .questions(quiz.getQuestions() != null
                        ? quiz.getQuestions().stream().map(q -> QuizQuestionResponse.builder()
                                .id(q.getId())
                                .text(q.getText())
                                .type(q.getType())
                                .points(q.getPoints())
                                .options(q.getOptions())
                                .correctIdx(q.getCorrectIdx() != null ? q.getCorrectIdx() : 0)
                                .correctText(q.getCorrectText())
                                .explanation(q.getExplanation())
                                .build())
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
