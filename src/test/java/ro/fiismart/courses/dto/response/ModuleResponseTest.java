package ro.fiismart.courses.dto.response;

import org.junit.jupiter.api.Test;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Lecture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleResponseTest {

    @Test
    void fromModel_null_returnsNull() {
        assertThat(ModuleResponse.fromModel(null)).isNull();
    }

    @Test
    void fromModel_withLectures_mapsAll() {
        Lecture lec = Lecture.builder().id("l1").title("Lec").type("video").videoUrl("http://v").build();
        CourseModule module = CourseModule.builder()
                .id("m1").title("Modul").description("Desc").order(2).quizId("q1")
                .lectures(List.of(lec)).build();

        ModuleResponse response = ModuleResponse.fromModel(module);

        assertThat(response.getId()).isEqualTo("m1");
        assertThat(response.getTitle()).isEqualTo("Modul");
        assertThat(response.getDescription()).isEqualTo("Desc");
        assertThat(response.getOrder()).isEqualTo(2);
        assertThat(response.getQuizId()).isEqualTo("q1");
        assertThat(response.getLectures()).hasSize(1);
    }

    @Test
    void fromModel_nullLectures_returnsEmptyList() {
        CourseModule module = CourseModule.builder().id("m1").lectures(null).build();

        ModuleResponse response = ModuleResponse.fromModel(module);

        assertThat(response.getLectures()).isEmpty();
    }
}
