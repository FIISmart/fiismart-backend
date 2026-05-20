package ro.fiismart.courses.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.fiismart.courses.dto.request.CreateCourseRequest;
import ro.fiismart.courses.dto.request.UpdateCourseRequest;
import ro.fiismart.courses.dto.response.CourseResponse;
import ro.fiismart.courses.service.CourseManagementService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock private CourseManagementService courseService;
    @InjectMocks private CourseController courseController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseController).build();
    }

    @Test
    void createCourse_returnsCreated() throws Exception {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setTitle("Matematică");
        req.setDescription("Descriere curs");
        req.setTeacherId("t1");
        CourseResponse resp = CourseResponse.builder().id("c1").title("Matematică").build();
        when(courseService.createCourse(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("c1"));
    }

    @Test
    void getCourse_returnsOk() throws Exception {
        when(courseService.getCourseById("c1")).thenReturn(CourseResponse.builder().id("c1").build());

        mockMvc.perform(get("/api/v1/courses/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("c1"));
    }

    @Test
    void getCourses_noParams_returnsAll() throws Exception {
        when(courseService.getAllCourses()).thenReturn(List.of(CourseResponse.builder().id("c1").build()));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("c1"));
    }

    @Test
    void getCourses_byTeacherId_returnsFiltered() throws Exception {
        when(courseService.getCoursesByTeacherId("t1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/courses?teacherId=t1"))
                .andExpect(status().isOk());
    }

    @Test
    void getCourses_publishedTrue_returnsPublished() throws Exception {
        when(courseService.getPublishedCourses()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/courses?published=true"))
                .andExpect(status().isOk());
    }

    @Test
    void updateCourse_returnsOk() throws Exception {
        UpdateCourseRequest req = new UpdateCourseRequest();
        req.setTitle("Nou");
        when(courseService.updateCourse(eq("c1"), any())).thenReturn(CourseResponse.builder().id("c1").build());

        mockMvc.perform(put("/api/v1/courses/c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void publishCourse_returnsOk() throws Exception {
        when(courseService.publishCourse("c1")).thenReturn(CourseResponse.builder().id("c1").build());

        mockMvc.perform(patch("/api/v1/courses/c1/publish"))
                .andExpect(status().isOk());
    }

    @Test
    void draftCourse_returnsOk() throws Exception {
        when(courseService.draftCourse("c1")).thenReturn(CourseResponse.builder().id("c1").build());

        mockMvc.perform(patch("/api/v1/courses/c1/draft"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCourse_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/c1"))
                .andExpect(status().isNoContent());

        verify(courseService).deleteCourse("c1");
    }
}
