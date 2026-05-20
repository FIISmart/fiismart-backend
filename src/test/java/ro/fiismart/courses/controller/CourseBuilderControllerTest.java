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
import ro.fiismart.courses.dto.request.*;
import ro.fiismart.courses.dto.response.*;
import ro.fiismart.courses.service.CourseManagementService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseBuilderControllerTest {

    @Mock private CourseManagementService courseService;
    @InjectMocks private CourseBuilderController courseBuilderController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseBuilderController).build();
    }

    @Test
    void getModules_returnsOk() throws Exception {
        when(courseService.getModules("c1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/courses/c1/builder/modules"))
                .andExpect(status().isOk());
    }

    @Test
    void addModule_returnsCreated() throws Exception {
        CreateModuleRequest req = new CreateModuleRequest();
        req.setTitle("Modul 1");
        ModuleResponse resp = ModuleResponse.builder().id("m1").title("Modul 1").build();
        when(courseService.addModule(eq("c1"), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/courses/c1/builder/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("m1"));
    }

    @Test
    void updateModule_returnsOk() throws Exception {
        CreateModuleRequest req = new CreateModuleRequest();
        req.setTitle("Updatat");
        ModuleResponse resp = ModuleResponse.builder().id("m1").build();
        when(courseService.updateModule(eq("c1"), eq("m1"), any())).thenReturn(resp);

        mockMvc.perform(put("/api/v1/courses/c1/builder/modules/m1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteModule_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/c1/builder/modules/m1"))
                .andExpect(status().isNoContent());

        verify(courseService).deleteModule("c1", "m1");
    }

    @Test
    void reorderModules_returnsOk() throws Exception {
        when(courseService.reorderModules(eq("c1"), any())).thenReturn(List.of());

        mockMvc.perform(put("/api/v1/courses/c1/builder/modules/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"m1\",\"m2\"]"))
                .andExpect(status().isOk());
    }

    @Test
    void addLecture_returnsCreated() throws Exception {
        CreateLectureRequest req = new CreateLectureRequest();
        req.setTitle("Lecție 1");
        req.setType("video");
        LectureResponse resp = LectureResponse.builder().id("l1").title("Lecție 1").build();
        when(courseService.addLectureToModule(eq("c1"), eq("m1"), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/courses/c1/builder/modules/m1/lectures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void updateLecture_returnsOk() throws Exception {
        UpdateLectureRequest req = new UpdateLectureRequest();
        req.setTitle("Updatat");
        LectureResponse resp = LectureResponse.builder().id("l1").build();
        when(courseService.updateLectureInModule(eq("c1"), eq("m1"), eq("l1"), any())).thenReturn(resp);

        mockMvc.perform(put("/api/v1/courses/c1/builder/modules/m1/lectures/l1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void removeLecture_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/c1/builder/modules/m1/lectures/l1"))
                .andExpect(status().isNoContent());

        verify(courseService).removeLectureFromModule("c1", "m1", "l1");
    }
}
