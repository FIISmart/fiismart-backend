package com.app.controller;

import com.app.dto.request.EnrollmentRequest;
import com.app.dto.response.EnrollmentResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.LectureProgress;
import com.app.service.EnrollmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnrollmentService enrollmentService;

    @Autowired
    private ObjectMapper objectMapper;

    private EnrollmentResponse buildEnrollmentResponse() {
        return EnrollmentResponse.builder()
                .id("e1")
                .studentId("s1")
                .courseId("c1")
                .enrolledAt(new Date())
                .status("enrolled")
                .overallProgress(0)
                .lectureProgress(new ArrayList<>())
                .build();
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(enrollmentService.create(any())).thenReturn(buildEnrollmentResponse());

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(EnrollmentRequest.builder()
                                .studentId("s1")
                                .courseId("c1")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("e1"))
                .andExpect(jsonPath("$.studentId").value("s1"))
                .andExpect(jsonPath("$.courseId").value("c1"))
                .andExpect(jsonPath("$.status").value("enrolled"));
    }

    @Test
    void create_blankStudentId_returns400() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"\",\"courseId\":\"c1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentId").exists());
    }

    @Test
    void create_blankCourseId_returns400() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"s1\",\"courseId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.courseId").exists());
    }

    @Test
    void create_serviceException_returns500() throws Exception {
        when(enrollmentService.create(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(EnrollmentRequest.builder()
                                .studentId("s1")
                                .courseId("c1")
                                .build())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void findById_exists_returns200() throws Exception {
        when(enrollmentService.findById("e1")).thenReturn(buildEnrollmentResponse());

        mockMvc.perform(get("/api/enrollments/{id}", "e1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("e1"))
                .andExpect(jsonPath("$.studentId").value("s1"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(enrollmentService.findById("missing"))
                .thenThrow(new ResourceNotFoundException("Enrollment", "missing"));

        mockMvc.perform(get("/api/enrollments/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findByStudentAndCourse_exists_returns200() throws Exception {
        when(enrollmentService.findByStudentAndCourse("s1", "c1")).thenReturn(buildEnrollmentResponse());

        mockMvc.perform(get("/api/enrollments/student/{studentId}/course/{courseId}", "s1", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("s1"))
                .andExpect(jsonPath("$.courseId").value("c1"));
    }

    @Test
    void findByStudentAndCourse_notFound_returns404() throws Exception {
        when(enrollmentService.findByStudentAndCourse("s1", "c1"))
                .thenThrow(new ResourceNotFoundException("Enrollment not found for student s1 and course c1"));

        mockMvc.perform(get("/api/enrollments/student/{studentId}/course/{courseId}", "s1", "c1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findByStudent_returns200() throws Exception {
        when(enrollmentService.findByStudentId("s1")).thenReturn(List.of(buildEnrollmentResponse()));

        mockMvc.perform(get("/api/enrollments/student/{studentId}", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].studentId").value("s1"));
    }

    @Test
    void findByStudent_emptyList_returns200() throws Exception {
        when(enrollmentService.findByStudentId("s99")).thenReturn(List.of());

        mockMvc.perform(get("/api/enrollments/student/{studentId}", "s99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findByCourse_returns200() throws Exception {
        when(enrollmentService.findByCourseId("c1")).thenReturn(List.of(buildEnrollmentResponse()));

        mockMvc.perform(get("/api/enrollments/course/{courseId}", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void findCompleted_returns200() throws Exception {
        EnrollmentResponse completed = buildEnrollmentResponse();
        completed.setStatus("completed");
        when(enrollmentService.findCompletedByStudent("s1")).thenReturn(List.of(completed));

        mockMvc.perform(get("/api/enrollments/student/{studentId}/completed", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("completed"));
    }

    @Test
    void findCompleted_emptyList_returns200() throws Exception {
        when(enrollmentService.findCompletedByStudent("s99")).thenReturn(List.of());

        mockMvc.perform(get("/api/enrollments/student/{studentId}/completed", "s99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void updateStatus_returns204() throws Exception {
        doNothing().when(enrollmentService).updateStatus(anyString(), anyString());

        mockMvc.perform(patch("/api/enrollments/{id}/status", "e1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"completed\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateProgress_returns204() throws Exception {
        doNothing().when(enrollmentService).updateOverallProgress(anyString(), anyInt());

        mockMvc.perform(patch("/api/enrollments/{id}/progress", "e1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"overallProgress\":75}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void markCompleted_returns204() throws Exception {
        doNothing().when(enrollmentService).markCompleted(anyString());

        mockMvc.perform(patch("/api/enrollments/{id}/complete", "e1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void addLectureProgress_returns204() throws Exception {
        doNothing().when(enrollmentService).addLectureProgress(anyString(), any(LectureProgress.class));

        LectureProgress progress = LectureProgress.builder()
                .lectureId("lec1")
                .watchedSecs(120)
                .completed(false)
                .lastWatchedAt(new Date())
                .build();

        mockMvc.perform(post("/api/enrollments/{id}/lecture-progress", "e1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progress)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_returns204() throws Exception {
        doNothing().when(enrollmentService).deleteById(anyString());

        mockMvc.perform(delete("/api/enrollments/{id}", "e1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteByStudentAndCourse_returns204() throws Exception {
        doNothing().when(enrollmentService).deleteByStudentAndCourse(anyString(), anyString());

        mockMvc.perform(delete("/api/enrollments/student/{studentId}/course/{courseId}", "s1", "c1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void isEnrolled_returnsTrue() throws Exception {
        when(enrollmentService.isEnrolled("s1", "c1")).thenReturn(true);

        mockMvc.perform(get("/api/enrollments/exists/student/{studentId}/course/{courseId}", "s1", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true));
    }

    @Test
    void isEnrolled_returnsFalse() throws Exception {
        when(enrollmentService.isEnrolled("s1", "c99")).thenReturn(false);

        mockMvc.perform(get("/api/enrollments/exists/student/{studentId}/course/{courseId}", "s1", "c99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(false));
    }

    @Test
    void countByCourse_returns200() throws Exception {
        when(enrollmentService.countByCourse("c1")).thenReturn(20L);

        mockMvc.perform(get("/api/enrollments/count/course/{courseId}", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(20));
    }

    @Test
    void countByCourse_returnsZero() throws Exception {
        when(enrollmentService.countByCourse("c99")).thenReturn(0L);

        mockMvc.perform(get("/api/enrollments/count/course/{courseId}", "c99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void findById_verifyAllFields() throws Exception {
        Date enrolledAt = new Date();
        Date completedAt = new Date();
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id("e1")
                .studentId("s1")
                .courseId("c1")
                .enrolledAt(enrolledAt)
                .completedAt(completedAt)
                .status("completed")
                .overallProgress(100)
                .lectureProgress(new ArrayList<>())
                .build();

        when(enrollmentService.findById("e1")).thenReturn(response);

        mockMvc.perform(get("/api/enrollments/{id}", "e1"))
                .andExpect(jsonPath("$.overallProgress").value(100))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.lectureProgress").isArray());
    }
}
