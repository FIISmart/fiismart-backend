package ro.fiismart.enrollment.controller;

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
import ro.fiismart.enrollment.dto.EnrollmentRequest;
import ro.fiismart.enrollment.dto.EnrollmentResponse;
import ro.fiismart.enrollment.service.EnrollmentService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentControllerTest {

    @Mock private EnrollmentService enrollmentService;
    @InjectMocks private EnrollmentController enrollmentController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(enrollmentController).build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        EnrollmentRequest req = new EnrollmentRequest();
        req.setStudentId("s1");
        req.setCourseId("c1");
        EnrollmentResponse resp = EnrollmentResponse.builder().id("e1").studentId("s1").courseId("c1").build();
        when(enrollmentService.create(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("e1"));
    }

    @Test
    void findById_returnsOk() throws Exception {
        EnrollmentResponse resp = EnrollmentResponse.builder().id("e1").build();
        when(enrollmentService.findById("e1")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/enrollments/e1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("e1"));
    }

    @Test
    void findByStudentAndCourse_returnsOk() throws Exception {
        EnrollmentResponse resp = EnrollmentResponse.builder().id("e1").build();
        when(enrollmentService.findByStudentAndCourse("s1", "c1")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/enrollments/student/s1/course/c1"))
                .andExpect(status().isOk());
    }

    @Test
    void findByStudent_returnsList() throws Exception {
        when(enrollmentService.findByStudentId("s1"))
                .thenReturn(List.of(EnrollmentResponse.builder().id("e1").build()));

        mockMvc.perform(get("/api/v1/enrollments/student/s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("e1"));
    }

    @Test
    void findByCourse_returnsList() throws Exception {
        when(enrollmentService.findByCourseId("c1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/enrollments/course/c1"))
                .andExpect(status().isOk());
    }

    @Test
    void findCompleted_returnsList() throws Exception {
        when(enrollmentService.findCompletedByStudent("s1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/enrollments/student/s1/completed"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/enrollments/e1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"completed\"}"))
                .andExpect(status().isNoContent());

        verify(enrollmentService).updateStatus("e1", "completed");
    }

    @Test
    void updateProgress_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/enrollments/e1/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"overallProgress\":75}"))
                .andExpect(status().isNoContent());

        verify(enrollmentService).updateOverallProgress("e1", 75);
    }

    @Test
    void markCompleted_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/enrollments/e1/complete"))
                .andExpect(status().isNoContent());

        verify(enrollmentService).markCompleted("e1");
    }

    @Test
    void deleteById_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/enrollments/e1"))
                .andExpect(status().isNoContent());

        verify(enrollmentService).deleteById("e1");
    }

    @Test
    void deleteByStudentAndCourse_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/enrollments/student/s1/course/c1"))
                .andExpect(status().isNoContent());

        verify(enrollmentService).deleteByStudentAndCourse("s1", "c1");
    }

    @Test
    void isEnrolled_returnsMap() throws Exception {
        when(enrollmentService.isEnrolled("s1", "c1")).thenReturn(true);

        mockMvc.perform(get("/api/v1/enrollments/exists/student/s1/course/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true));
    }

    @Test
    void countByCourse_returnsMap() throws Exception {
        when(enrollmentService.countByCourse("c1")).thenReturn(42L);

        mockMvc.perform(get("/api/v1/enrollments/count/course/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(42));
    }
}
