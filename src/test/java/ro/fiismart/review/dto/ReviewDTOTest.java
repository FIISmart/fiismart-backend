package ro.fiismart.review.dto;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewDTOTest {

    @Test
    void reviewRequest_gettersSetters() {
        ReviewRequest req = new ReviewRequest();
        req.setStudentId("s1");
        req.setCourseId("c1");
        req.setStars(4);
        req.setBody("Bun!");
        assertThat(req.getStudentId()).isEqualTo("s1");
        assertThat(req.getStars()).isEqualTo(4);
        assertThat(req.getBody()).isEqualTo("Bun!");
    }

    @Test
    void reviewResponse_builder() {
        ReviewResponse resp = ReviewResponse.builder()
                .id("r1").studentId("s1").courseId("c1")
                .stars(5).body("Excelent!").deleted(false)
                .createdAt(new Date()).build();
        assertThat(resp.getId()).isEqualTo("r1");
        assertThat(resp.getStars()).isEqualTo(5);
        assertThat(resp.isDeleted()).isFalse();
    }
}
