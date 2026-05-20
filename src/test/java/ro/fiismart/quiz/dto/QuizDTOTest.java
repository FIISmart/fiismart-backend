package ro.fiismart.quiz.dto;

import org.junit.jupiter.api.Test;
import ro.fiismart.quiz.dto.modulequiz.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizDTOTest {

    @Test
    void quizRequest_gettersSetters() {
        QuizRequest req = new QuizRequest();
        req.setCourseId("c1");
        req.setTitle("Quiz");
        req.setPassingScore(70);
        req.setTimeLimit(30);
        req.setShuffleQuestions(true);
        assertThat(req.getCourseId()).isEqualTo("c1");
        assertThat(req.getPassingScore()).isEqualTo(70);
        assertThat(req.isShuffleQuestions()).isTrue();
    }

    @Test
    void quizResponse_builder() {
        QuizResponse resp = QuizResponse.builder()
                .id("q1").courseId("c1").title("Quiz")
                .passingScore(70).timeLimit(30).shuffleQuestions(false)
                .questions(List.of())
                .build();
        assertThat(resp.getId()).isEqualTo("q1");
        assertThat(resp.getPassingScore()).isEqualTo(70);
    }

    @Test
    void quizQuestionRequest_gettersSetters() {
        QuizQuestionRequest req = new QuizQuestionRequest();
        req.setText("Întrebare?");
        req.setType("single");
        req.setPoints(2);
        req.setOptions(List.of("A", "B"));
        req.setCorrectIdx(0);
        assertThat(req.getText()).isEqualTo("Întrebare?");
        assertThat(req.getCorrectIdx()).isEqualTo(0);
    }

    @Test
    void createModuleQuizRequest_gettersSetters() {
        CreateModuleQuizRequest req = new CreateModuleQuizRequest();
        req.setTitle("Quiz M");
        req.setPassingScore(60);
        req.setTimeLimit(20);
        req.setShuffleQuestions(true);
        assertThat(req.getTitle()).isEqualTo("Quiz M");
        assertThat(req.isShuffleQuestions()).isTrue();
    }

    @Test
    void moduleQuizQuestionRequest_gettersSetters() {
        ModuleQuizQuestionRequest req = new ModuleQuizQuestionRequest();
        req.setText("Q?");
        req.setType("single");
        req.setPoints(1);
        req.setOptions(List.of("X", "Y"));
        req.setCorrectIdx(1);
        req.setImageUrl("img.png");
        assertThat(req.getText()).isEqualTo("Q?");
        assertThat(req.getImageUrl()).isEqualTo("img.png");
    }

    @Test
    void moduleQuizResponse_builder() {
        ModuleQuizResponse resp = ModuleQuizResponse.builder()
                .id("mq1").courseId("c1").title("Quiz L")
                .quizScope("lecture").passingScore(70)
                .build();
        assertThat(resp.getId()).isEqualTo("mq1");
        assertThat(resp.getQuizScope()).isEqualTo("lecture");
    }
}
