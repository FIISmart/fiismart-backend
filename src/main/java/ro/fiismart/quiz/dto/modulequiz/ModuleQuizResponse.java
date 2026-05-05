package ro.fiismart.quiz.dto.modulequiz;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.ModuleQuiz;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class ModuleQuizResponse {

    private String id;
    private String courseId;
    private String moduleId;
    private String lectureId;
    /** "lecture" | "module" | "course_final" */
    private String quizScope;
    private String title;
    private int passingScore;
    private int timeLimit;
    private boolean shuffleQuestions;
    private List<ModuleQuizQuestionResponse> questions;

    public static ModuleQuizResponse fromModel(ModuleQuiz quiz) {
        if (quiz == null) return null;
        return ModuleQuizResponse.builder()
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
                        ? quiz.getQuestions().stream()
                        .map(ModuleQuizQuestionResponse::fromModel)
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
