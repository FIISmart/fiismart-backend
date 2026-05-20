package ro.fiismart.dashboard.student.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.dashboard.student.dto.RecommendationDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentRecommendationServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private StudentRecommendationService studentRecommendationService;

    @Test
    void getRecommendation_allCoursesEnrolled_returnsNull() {
        Enrollment e1 = Enrollment.builder().courseId("c1").build();
        Course c1 = Course.builder().id("c1").build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1));
        when(courseRepository.findAll()).thenReturn(List.of(c1));

        RecommendationDTO result = studentRecommendationService.getRecommendation("s1");

        assertThat(result).isNull();
    }

    @Test
    void getRecommendation_hasUnenrolledCourses_returnsRecommendation() {
        Enrollment e1 = Enrollment.builder().courseId("c1").build();
        Course c1 = Course.builder().id("c1").title("Curs 1").build();
        Course c2 = Course.builder().id("c2").title("Curs 2").description("Desc 2")
                .thumbnailUrl("thumb.jpg").avgRating(4.0).enrollmentCount(10).build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1));
        when(courseRepository.findAll()).thenReturn(List.of(c1, c2));

        RecommendationDTO result = studentRecommendationService.getRecommendation("s1");

        assertThat(result).isNotNull();
        assertThat(result.getCourseId()).isEqualTo("c2");
        assertThat(result.getTitle()).isEqualTo("Curs 2");
    }

    @Test
    void getRecommendation_noEnrollments_returnsSomeCourse() {
        Course c1 = Course.builder().id("c1").title("Curs 1").build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of());
        when(courseRepository.findAll()).thenReturn(List.of(c1));

        RecommendationDTO result = studentRecommendationService.getRecommendation("s1");

        assertThat(result).isNotNull();
        assertThat(result.getCourseId()).isEqualTo("c1");
    }

    @Test
    void getRecommendation_noCourses_returnsNull() {
        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of());
        when(courseRepository.findAll()).thenReturn(List.of());

        RecommendationDTO result = studentRecommendationService.getRecommendation("s1");

        assertThat(result).isNull();
    }
}
