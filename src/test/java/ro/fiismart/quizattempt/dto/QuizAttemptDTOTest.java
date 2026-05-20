package ro.fiismart.quizattempt.dto;

import org.junit.jupiter.api.Test;
import ro.fiismart.common.model.Answer;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizAttemptDTOTest {

    @Test
    void quizAttemptRequest_gettersSetters() {
        QuizAttemptRequest req = new QuizAttemptRequest();
        req.setStudentId("s1");
        req.setQuizId("q1");
        req.setCourseId("c1");
        req.setTimeTakenSecs(120);
        Answer a = new Answer();
        a.setQuestionId("qq1");
        a.setSelectedIdx(0);
        req.setAnswers(List.of(a));
        assertThat(req.getStudentId()).isEqualTo("s1");
        assertThat(req.getTimeTakenSecs()).isEqualTo(120);
        assertThat(req.getAnswers()).hasSize(1);
    }

    @Test
    void quizAttemptResponse_builder() {
        QuizAttemptResponse resp = QuizAttemptResponse.builder()
                .id("a1").quizId("q1").courseId("c1").studentId("s1")
                .score(85).passed(true).timeTakenSecs(90)
                .attemptedAt(new Date())
                .build();
        assertThat(resp.getId()).isEqualTo("a1");
        assertThat(resp.getScore()).isEqualTo(85);
        assertThat(resp.isPassed()).isTrue();
    }
}
