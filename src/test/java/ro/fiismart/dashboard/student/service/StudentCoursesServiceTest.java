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
import ro.fiismart.dashboard.student.dto.CourseSummaryDTO;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCoursesServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private StudentCoursesService studentCoursesService;

    @Test
    void getCourses_noEnrollments_returnsEmpty() {
        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of());

        List<CourseSummaryDTO> result = studentCoursesService.getCourses("s1");

        assertThat(result).isEmpty();
    }

    @Test
    void getCourses_withEnrollments_returnsMappedDTOs() {
        Enrollment e1 = Enrollment.builder().courseId("c1").overallProgress(40).status("enrolled").build();
        Course c1 = Course.builder().id("c1").title("Fizică").description("Desc").
                thumbnailUrl("thumb.jpg").avgRating(4.2).enrollmentCount(30).build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1));
        when(courseRepository.findById("c1")).thenReturn(Optional.of(c1));

        List<CourseSummaryDTO> result = studentCoursesService.getCourses("s1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseId()).isEqualTo("c1");
        assertThat(result.get(0).getTitle()).isEqualTo("Fizică");
        assertThat(result.get(0).getOverallProgress()).isEqualTo(40);
        assertThat(result.get(0).getStatus()).isEqualTo("enrolled");
    }

    @Test
    void getCourses_courseNotFound_skipsEnrollment() {
        Enrollment e1 = Enrollment.builder().courseId("noId").build();

        when(enrollmentRepository.findByStudentId("s1")).thenReturn(List.of(e1));
        when(courseRepository.findById("noId")).thenReturn(Optional.empty());

        List<CourseSummaryDTO> result = studentCoursesService.getCourses("s1");

        assertThat(result).isEmpty();
    }
}
