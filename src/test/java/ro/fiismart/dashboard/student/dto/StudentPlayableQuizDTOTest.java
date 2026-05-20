package ro.fiismart.dashboard.student.dto;

import org.junit.jupiter.api.Test;
import ro.fiismart.common.model.ModuleQuiz;
import ro.fiismart.common.model.ModuleQuizQuestion;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.common.model.QuizQuestion;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudentPlayableQuizDTOTest {

    @Test
    void fromModuleQuiz_mapsAllFields() {
        ModuleQuizQuestion q = ModuleQuizQuestion.builder()
                .id("q1").text("Ce este 2+2?").type("single").points(1).build();
        ModuleQuiz mq = ModuleQuiz.builder()
                .id("mq1").courseId("c1").moduleId("m1").lectureId("l1")
                .quizScope("lecture").title("Quiz L").passingScore(70).timeLimit(30)
                .shuffleQuestions(true).questions(List.of(q)).build();

        StudentPlayableQuizDTO dto = StudentPlayableQuizDTO.fromModuleQuiz(mq);

        assertThat(dto.getId()).isEqualTo("mq1");
        assertThat(dto.getCourseId()).isEqualTo("c1");
        assertThat(dto.getModuleId()).isEqualTo("m1");
        assertThat(dto.getLectureId()).isEqualTo("l1");
        assertThat(dto.getQuizScope()).isEqualTo("lecture");
        assertThat(dto.getTitle()).isEqualTo("Quiz L");
        assertThat(dto.getPassingScore()).isEqualTo(70);
        assertThat(dto.getTimeLimit()).isEqualTo(30);
        assertThat(dto.isShuffleQuestions()).isTrue();
        assertThat(dto.getQuestions()).hasSize(1);
    }

    @Test
    void fromModuleQuiz_nullQuestions_returnsEmptyList() {
        ModuleQuiz mq = ModuleQuiz.builder().id("mq2").questions(null).build();

        StudentPlayableQuizDTO dto = StudentPlayableQuizDTO.fromModuleQuiz(mq);

        assertThat(dto.getQuestions()).isEmpty();
    }

    @Test
    void fromLegacyQuiz_mapsAllFields() {
        QuizQuestion q = QuizQuestion.builder()
                .id("q1").text("Cât face Pi?").type("single").points(1).build();
        Quiz quiz = Quiz.builder()
                .id("qz1").courseId("c1").title("Final Quiz")
                .passingScore(60).timeLimit(45).shuffleQuestions(false)
                .questions(List.of(q)).build();

        StudentPlayableQuizDTO dto = StudentPlayableQuizDTO.fromLegacyQuiz(quiz);

        assertThat(dto.getId()).isEqualTo("qz1");
        assertThat(dto.getCourseId()).isEqualTo("c1");
        assertThat(dto.getQuizScope()).isEqualTo("course_final");
        assertThat(dto.getTitle()).isEqualTo("Final Quiz");
        assertThat(dto.getPassingScore()).isEqualTo(60);
        assertThat(dto.isShuffleQuestions()).isFalse();
        assertThat(dto.getQuestions()).hasSize(1);
    }

    @Test
    void fromLegacyQuiz_nullQuestions_returnsEmptyList() {
        Quiz quiz = Quiz.builder().id("qz2").questions(null).build();

        StudentPlayableQuizDTO dto = StudentPlayableQuizDTO.fromLegacyQuiz(quiz);

        assertThat(dto.getQuestions()).isEmpty();
    }
}
