package ro.fiismart.dashboard.student.controller;

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
import ro.fiismart.dashboard.student.dto.*;
import ro.fiismart.dashboard.student.service.StudentLectureService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudentLectureControllerTest {

    @Mock private StudentLectureService studentLectureService;
    @InjectMocks private StudentLectureController studentLectureController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studentLectureController).build();
    }

    @Test
    void getModules_returnsOk() throws Exception {
        when(studentLectureService.getModules("s1", "c1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/students/s1/courses/c1/modules"))
                .andExpect(status().isOk());
    }

    @Test
    void getLectureDetail_returnsOk() throws Exception {
        StudentLectureDetailDTO dto = new StudentLectureDetailDTO();
        when(studentLectureService.getLectureDetail("s1", "c1", "l1")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/students/s1/courses/c1/lectures/l1"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProgress_returnsOk() throws Exception {
        StudentLectureProgressResponse resp = new StudentLectureProgressResponse();
        when(studentLectureService.updateLectureProgress(eq("s1"), eq("c1"), eq("l1"), any()))
                .thenReturn(resp);

        StudentLectureProgressRequest req = new StudentLectureProgressRequest();
        req.setWatchedPercent(50);
        req.setPositionSecs(150);

        mockMvc.perform(put("/api/v1/students/s1/courses/c1/lectures/l1/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
