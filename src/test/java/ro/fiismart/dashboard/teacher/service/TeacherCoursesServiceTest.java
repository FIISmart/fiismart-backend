package ro.fiismart.dashboard.teacher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.dashboard.teacher.dto.TeacherCoursesDTO;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherCoursesServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private TeacherCoursesService teacherCoursesService;

    @Test
    void getCourses_allStatus_returnsAll() {
        Course c1 = Course.builder().id("c1").title("Curs 1").status("published")
                .updatedAt(new Date()).build();
        Course c2 = Course.builder().id("c2").title("Curs 2").status("draft")
                .updatedAt(new Date()).build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(new java.util.ArrayList<>(List.of(c1, c2)));

        List<TeacherCoursesDTO> result = teacherCoursesService.getCourses("teacher1", "all", 10, 0);

        assertThat(result).hasSize(2);
    }

    @Test
    void getCourses_filterByStatus_returnsFiltered() {
        Course c1 = Course.builder().id("c1").title("Publicat").status("published")
                .updatedAt(new Date()).build();
        Course c2 = Course.builder().id("c2").title("Draft").status("draft")
                .updatedAt(new Date()).build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(new java.util.ArrayList<>(List.of(c1, c2)));

        List<TeacherCoursesDTO> result = teacherCoursesService.getCourses("teacher1", "published", 10, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Publicat");
    }

    @Test
    void getCourses_withPagination_appliesLimitAndOffset() {
        Course c1 = Course.builder().id("c1").title("A").status("published")
                .updatedAt(new Date(1000)).build();
        Course c2 = Course.builder().id("c2").title("B").status("published")
                .updatedAt(new Date(2000)).build();
        Course c3 = Course.builder().id("c3").title("C").status("published")
                .updatedAt(new Date(3000)).build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(new java.util.ArrayList<>(List.of(c1, c2, c3)));

        List<TeacherCoursesDTO> result = teacherCoursesService.getCourses("teacher1", "all", 2, 1);

        assertThat(result).hasSize(2);
    }

    @Test
    void getCourses_nullUpdatedAt_handledGracefully() {
        Course c1 = Course.builder().id("c1").title("Fara data").status("draft")
                .updatedAt(null).build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(new java.util.ArrayList<>(List.of(c1)));

        List<TeacherCoursesDTO> result = teacherCoursesService.getCourses("teacher1", "all", 10, 0);

        assertThat(result).hasSize(1);
    }

    @Test
    void getCourses_mapsAllFields() {
        Course c1 = Course.builder().id("c1").title("Fizică").description("Curs fizică")
                .status("published").enrollmentCount(50).avgRating(4.5)
                .thumbnailUrl("thumb.jpg").updatedAt(new Date()).build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(new java.util.ArrayList<>(List.of(c1)));

        List<TeacherCoursesDTO> result = teacherCoursesService.getCourses("teacher1", "all", 10, 0);

        assertThat(result.get(0).getCourseId()).isEqualTo("c1");
        assertThat(result.get(0).getDescription()).isEqualTo("Curs fizică");
        assertThat(result.get(0).getEnrollmentCount()).isEqualTo(50);
        assertThat(result.get(0).getAvgRating()).isEqualTo(4.5);
        assertThat(result.get(0).getThumbnailUrl()).isEqualTo("thumb.jpg");
    }
}
