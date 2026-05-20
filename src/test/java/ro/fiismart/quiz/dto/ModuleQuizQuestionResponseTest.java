package ro.fiismart.quiz.dto;

import org.junit.jupiter.api.Test;
import ro.fiismart.common.model.ModuleQuizQuestion;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizQuestionResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleQuizQuestionResponseTest {

    @Test
    void fromModel_null_returnsNull() {
        assertThat(ModuleQuizQuestionResponse.fromModel(null)).isNull();
    }

    @Test
    void fromModel_mapsAllFields() {
        ModuleQuizQuestion q = ModuleQuizQuestion.builder()
                .id("q1").text("Întrebare?").imageUrl("img.png").type("single")
                .points(2).options(List.of("A", "B")).correctIdx(0)
                .correctText("A").explanation("Pentru că...").build();

        ModuleQuizQuestionResponse r = ModuleQuizQuestionResponse.fromModel(q);

        assertThat(r.getId()).isEqualTo("q1");
        assertThat(r.getText()).isEqualTo("Întrebare?");
        assertThat(r.getImageUrl()).isEqualTo("img.png");
        assertThat(r.getType()).isEqualTo("single");
        assertThat(r.getPoints()).isEqualTo(2);
        assertThat(r.getOptions()).containsExactly("A", "B");
        assertThat(r.getCorrectIdx()).isEqualTo(0);
        assertThat(r.getCorrectText()).isEqualTo("A");
        assertThat(r.getExplanation()).isEqualTo("Pentru că...");
    }
}
