package ro.fiismart.dashboard.student.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.ModuleQuiz;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizQuestionResponse;
import ro.fiismart.quiz.dto.QuizQuestionResponse;

import java.util.List;

@Data
@Builder
public class StudentPlayableQuizDTO {
    private String id;
    private String courseId;
    private String moduleId;
    private String lectureId;
    private String quizScope;
    private String title;
    private int passingScore;
    private int timeLimit;
    private boolean shuffleQuestions;
    private List<?> questions;

    public static StudentPlayableQuizDTO fromModuleQuiz(ModuleQuiz quiz) {
        return StudentPlayableQuizDTO.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
                .moduleId(quiz.getModuleId())
                .lectureId(quiz.getLectureId())
                .quizScope(quiz.getQuizScope())
                .title(quiz.getTitle())
                .passingScore(quiz.getPassingScore())
                .timeLimit(quiz.getTimeLimit())
                .shuffleQuestions(quiz.isShuffleQuestions())
                .questions(quiz.getQuestions() != null
                        ? quiz.getQuestions().stream().map(ModuleQuizQuestionResponse::fromModel).toList()
                        : List.of())
                .build();
    }

    public static StudentPlayableQuizDTO fromLegacyQuiz(Quiz quiz) {
        return StudentPlayableQuizDTO.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
                .quizScope("course_final")
                .title(quiz.getTitle())
                .passingScore(quiz.getPassingScore())
                .timeLimit(quiz.getTimeLimit())
                .shuffleQuestions(quiz.isShuffleQuestions())
                .questions(quiz.getQuestions() != null
                        ? quiz.getQuestions().stream().map(QuizQuestionResponse::fromModel).toList()
                        : List.of())
                .build();
    }
}
