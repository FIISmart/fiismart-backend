package ro.fiismart.enrollment.dto;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentDTOTest {

    @Test
    void enrollmentRequest_gettersSetters() {
        EnrollmentRequest req = new EnrollmentRequest();
        req.setStudentId("s1");
        req.setCourseId("c1");
        assertThat(req.getStudentId()).isEqualTo("s1");
        assertThat(req.getCourseId()).isEqualTo("c1");
    }

    @Test
    void enrollmentResponse_builder() {
        Date now = new Date();
        EnrollmentResponse resp = EnrollmentResponse.builder()
                .id("e1").studentId("s1").courseId("c1")
                .status("enrolled").overallProgress(50)
                .enrolledAt(now).lastAccessedAt(now).completedAt(now)
                .build();
        assertThat(resp.getId()).isEqualTo("e1");
        assertThat(resp.getStatus()).isEqualTo("enrolled");
        assertThat(resp.getOverallProgress()).isEqualTo(50);
    }
}
