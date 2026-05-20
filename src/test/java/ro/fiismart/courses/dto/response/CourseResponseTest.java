package ro.fiismart.courses.dto.response;

import org.junit.jupiter.api.Test;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Lecture;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseResponseTest {

    @Test
    void fromModel_null_returnsNull() {
        assertThat(CourseResponse.fromModel(null)).isNull();
    }

    @Test
    void fromModel_fullCourse_mapsAllFields() {
        Date now = new Date();
        Lecture lec = Lecture.builder().id("l1").title("L1").type("video").videoUrl("http://v").build();
        CourseModule mod = CourseModule.builder().id("m1").title("M1").order(0)
                .lectures(List.of(lec)).build();
        Course course = Course.builder()
                .id("c1").title("T").description("D").teacherId("t1").status("published")
                .tags(List.of("math")).thumbnailUrl("thumb.jpg").language("ro")
                .enrollmentCount(10).avgRating(4.5).hidden(false).quizId("q1")
                .createdAt(now).updatedAt(now)
                .modules(List.of(mod)).build();

        CourseResponse response = CourseResponse.fromModel(course);

        assertThat(response.getId()).isEqualTo("c1");
        assertThat(response.getTitle()).isEqualTo("T");
        assertThat(response.getDescription()).isEqualTo("D");
        assertThat(response.getTeacherId()).isEqualTo("t1");
        assertThat(response.getStatus()).isEqualTo("published");
        assertThat(response.getTags()).containsExactly("math");
        assertThat(response.getEnrollmentCount()).isEqualTo(10);
        assertThat(response.getAvgRating()).isEqualTo(4.5);
        assertThat(response.getQuizId()).isEqualTo("q1");
        assertThat(response.getModules()).hasSize(1);
        assertThat(response.getModules().get(0).getId()).isEqualTo("m1");
    }

    @Test
    void fromModel_nullModules_returnsEmptyList() {
        Course course = Course.builder().id("c1").modules(null).build();

        CourseResponse response = CourseResponse.fromModel(course);

        assertThat(response.getModules()).isEmpty();
    }
}
