package ro.fiismart.quiz.dto;

import org.junit.jupiter.api.Test;
import ro.fiismart.common.model.QuizQuestion;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizQuestionResponseTest {

    @Test
    void fromModel_null_returnsNull() {
        assertThat(QuizQuestionResponse.fromModel(null)).isNull();
    }

    @Test
    void fromModel_mapsAllFields() {
        QuizQuestion q = QuizQuestion.builder()
                .id("q1").text("Cât face 2+2?").type("single")
                .points(1).options(List.of("3", "4")).correctIdx(1)
                .correctText("4").explanation("2+2=4").build();

        QuizQuestionResponse r = QuizQuestionResponse.fromModel(q);

        assertThat(r.getId()).isEqualTo("q1");
        assertThat(r.getText()).isEqualTo("Cât face 2+2?");
        assertThat(r.getType()).isEqualTo("single");
        assertThat(r.getPoints()).isEqualTo(1);
        assertThat(r.getOptions()).containsExactly("3", "4");
        assertThat(r.getCorrectIdx()).isEqualTo(1);
        assertThat(r.getCorrectText()).isEqualTo("4");
        assertThat(r.getExplanation()).isEqualTo("2+2=4");
    }
}
