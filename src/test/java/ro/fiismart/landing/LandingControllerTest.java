package ro.fiismart.landing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LandingControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @InjectMocks private LandingController landingController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(landingController).build();
    }

    @Test
    void getStatistics_returnsStats() throws Exception {
        when(userRepository.countByRole("student")).thenReturn(100L);
        when(userRepository.countByRole("teacher")).thenReturn(10L);
        Course visible = Course.builder().id("c1").hidden(false).build();
        Course hidden = Course.builder().id("c2").hidden(true).build();
        when(courseRepository.findAll()).thenReturn(List.of(visible, hidden));

        mockMvc.perform(get("/api/v1/landing/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudents").value(100))
                .andExpect(jsonPath("$.freeCourses").value(1));
    }

    @Test
    void getCategories_returnsGroupedTags() throws Exception {
        Course c1 = Course.builder().id("c1").tags(List.of("math")).hidden(false).build();
        Course c2 = Course.builder().id("c2").tags(List.of("math")).hidden(false).build();
        Course c3 = Course.builder().id("c3").tags(List.of("science")).hidden(false).build();
        when(courseRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        mockMvc.perform(get("/api/v1/landing/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.math").value(2))
                .andExpect(jsonPath("$.Toate").value(3));
    }

    @Test
    void getPopularCourses_returnsUpTo4() throws Exception {
        List<Course> courses = List.of(
                Course.builder().id("c1").title("C1").hidden(false).build(),
                Course.builder().id("c2").title("C2").hidden(false).build(),
                Course.builder().id("c3").title("C3").hidden(false).build(),
                Course.builder().id("c4").title("C4").hidden(false).build(),
                Course.builder().id("c5").title("C5").hidden(false).build()
        );
        when(courseRepository.findAll()).thenReturn(courses);

        mockMvc.perform(get("/api/v1/landing/courses/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void getPopularCourses_hiddenExcluded() throws Exception {
        Course visible = Course.builder().id("c1").title("Vis").hidden(false).build();
        Course hidden = Course.builder().id("c2").title("Hid").hidden(true).build();
        when(courseRepository.findAll()).thenReturn(List.of(visible, hidden));

        mockMvc.perform(get("/api/v1/landing/courses/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Vis"));
    }
}
