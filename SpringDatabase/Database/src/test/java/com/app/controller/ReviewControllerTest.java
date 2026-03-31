package com.app.controller;

import com.app.dto.request.ReviewRequest;
import com.app.dto.response.ReviewResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    private ReviewResponse buildReviewResponse() {
        return ReviewResponse.builder()
                .id("r1")
                .studentId("s1")
                .courseId("c1")
                .stars(5)
                .body("Excellent course!")
                .createdAt(new Date())
                .deleted(false)
                .build();
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(reviewService.create(any())).thenReturn(buildReviewResponse());

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ReviewRequest.builder()
                                .studentId("s1")
                                .courseId("c1")
                                .stars(5)
                                .body("Excellent course!")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("r1"))
                .andExpect(jsonPath("$.studentId").value("s1"))
                .andExpect(jsonPath("$.courseId").value("c1"))
                .andExpect(jsonPath("$.stars").value(5))
                .andExpect(jsonPath("$.body").value("Excellent course!"));
    }

    @Test
    void create_blankStudentId_returns400() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"\",\"courseId\":\"c1\",\"stars\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentId").exists());
    }

    @Test
    void create_blankCourseId_returns400() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"s1\",\"courseId\":\"\",\"stars\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.courseId").exists());
    }

    @Test
    void create_starsZero_returns400() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"s1\",\"courseId\":\"c1\",\"stars\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.stars").exists());
    }

    @Test
    void create_starsSix_returns400() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"s1\",\"courseId\":\"c1\",\"stars\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.stars").exists());
    }

    @Test
    void create_starsNegative_returns400() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"s1\",\"courseId\":\"c1\",\"stars\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_serviceException_returns500() throws Exception {
        when(reviewService.create(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ReviewRequest.builder()
                                .studentId("s1")
                                .courseId("c1")
                                .stars(4)
                                .build())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void findById_exists_returns200() throws Exception {
        when(reviewService.findById("r1")).thenReturn(buildReviewResponse());

        mockMvc.perform(get("/api/reviews/{id}", "r1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("r1"))
                .andExpect(jsonPath("$.stars").value(5));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(reviewService.findById("missing")).thenThrow(new ResourceNotFoundException("Review", "missing"));

        mockMvc.perform(get("/api/reviews/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void findById_serviceException_returns500() throws Exception {
        when(reviewService.findById("error")).thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/reviews/{id}", "error"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void findByStudentAndCourse_exists_returns200() throws Exception {
        when(reviewService.findByStudentAndCourse("s1", "c1")).thenReturn(buildReviewResponse());

        mockMvc.perform(get("/api/reviews/student/{studentId}/course/{courseId}", "s1", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("s1"))
                .andExpect(jsonPath("$.courseId").value("c1"));
    }

    @Test
    void findByStudentAndCourse_notFound_returns404() throws Exception {
        when(reviewService.findByStudentAndCourse("s1", "c99"))
                .thenThrow(new ResourceNotFoundException("Review not found for student s1 and course c99"));

        mockMvc.perform(get("/api/reviews/student/{studentId}/course/{courseId}", "s1", "c99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findByCourse_returns200() throws Exception {
        when(reviewService.findByCourseId("c1")).thenReturn(List.of(buildReviewResponse()));

        mockMvc.perform(get("/api/reviews/course/{courseId}", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].courseId").value("c1"));
    }

    @Test
    void findByCourse_emptyList_returns200() throws Exception {
        when(reviewService.findByCourseId("c99")).thenReturn(List.of());

        mockMvc.perform(get("/api/reviews/course/{courseId}", "c99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findByStudent_returns200() throws Exception {
        when(reviewService.findByStudentId("s1")).thenReturn(List.of(buildReviewResponse()));

        mockMvc.perform(get("/api/reviews/student/{studentId}", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value("s1"));
    }

    @Test
    void findByStudent_emptyList_returns200() throws Exception {
        when(reviewService.findByStudentId("s99")).thenReturn(List.of());

        mockMvc.perform(get("/api/reviews/student/{studentId}", "s99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findByCourseAndStars_returns200() throws Exception {
        when(reviewService.findByCourseAndStars("c1", 5)).thenReturn(List.of(buildReviewResponse()));

        mockMvc.perform(get("/api/reviews/course/{courseId}/stars/{stars}", "c1", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stars").value(5));
    }

    @Test
    void findByCourseAndStars_emptyList_returns200() throws Exception {
        when(reviewService.findByCourseAndStars("c1", 1)).thenReturn(List.of());

        mockMvc.perform(get("/api/reviews/course/{courseId}/stars/{stars}", "c1", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void computeAvgRating_returns200() throws Exception {
        when(reviewService.computeAvgRating("c1")).thenReturn(4.3);

        mockMvc.perform(get("/api/reviews/course/{courseId}/avg-rating", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgRating").value(4.3));
    }

    @Test
    void computeAvgRating_zero_returns200() throws Exception {
        when(reviewService.computeAvgRating("c99")).thenReturn(0.0);

        mockMvc.perform(get("/api/reviews/course/{courseId}/avg-rating", "c99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgRating").value(0.0));
    }

    @Test
    void updateReview_returns204() throws Exception {
        doNothing().when(reviewService).updateReview(anyString(), anyInt(), anyString());

        mockMvc.perform(patch("/api/reviews/{id}", "r1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stars\":4,\"body\":\"Updated review\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void softDelete_returns204() throws Exception {
        doNothing().when(reviewService).softDelete(anyString(), anyString());

        mockMvc.perform(patch("/api/reviews/{id}/soft-delete", "r1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deletedBy\":\"adminId\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_returns204() throws Exception {
        doNothing().when(reviewService).deleteById(anyString());

        mockMvc.perform(delete("/api/reviews/{id}", "r1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_serviceException_returns500() throws Exception {
        doThrow(new RuntimeException("DB error")).when(reviewService).deleteById("error");

        mockMvc.perform(delete("/api/reviews/{id}", "error"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void countByCourse_returns200() throws Exception {
        when(reviewService.countByCourse("c1")).thenReturn(20L);

        mockMvc.perform(get("/api/reviews/count/course/{courseId}", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(20));
    }

    @Test
    void countByCourse_returnsZero() throws Exception {
        when(reviewService.countByCourse("c99")).thenReturn(0L);

        mockMvc.perform(get("/api/reviews/count/course/{courseId}", "c99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void findById_verifyAllFields() throws Exception {
        Date createdAt = new Date();
        ReviewResponse response = ReviewResponse.builder()
                .id("r1")
                .studentId("s1")
                .courseId("c1")
                .stars(4)
                .body("Good course")
                .createdAt(createdAt)
                .deleted(false)
                .deletedBy(null)
                .build();

        when(reviewService.findById("r1")).thenReturn(response);

        mockMvc.perform(get("/api/reviews/{id}", "r1"))
                .andExpect(jsonPath("$.id").value("r1"))
                .andExpect(jsonPath("$.studentId").value("s1"))
                .andExpect(jsonPath("$.courseId").value("c1"))
                .andExpect(jsonPath("$.stars").value(4))
                .andExpect(jsonPath("$.body").value("Good course"))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void findById_deletedReview_showsDeletedBy() throws Exception {
        ReviewResponse deleted = ReviewResponse.builder()
                .id("r1")
                .studentId("s1")
                .courseId("c1")
                .stars(1)
                .body("Bad")
                .createdAt(new Date())
                .deleted(true)
                .deletedBy("adminId")
                .build();

        when(reviewService.findById("r1")).thenReturn(deleted);

        mockMvc.perform(get("/api/reviews/{id}", "r1"))
                .andExpect(jsonPath("$.deleted").value(true))
                .andExpect(jsonPath("$.deletedBy").value("adminId"));
    }

    @Test
    void create_withNullBody_returns201() throws Exception {
        ReviewResponse response = buildReviewResponse();
        response.setBody(null);
        when(reviewService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"s1\",\"courseId\":\"c1\",\"stars\":3}"))
                .andExpect(status().isCreated());
    }

    @Test
    void create_minStarsOne_returns201() throws Exception {
        ReviewResponse response = buildReviewResponse();
        response.setStars(1);
        when(reviewService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"s1\",\"courseId\":\"c1\",\"stars\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stars").value(1));
    }

    @Test
    void create_maxStarsFive_returns201() throws Exception {
        when(reviewService.create(any())).thenReturn(buildReviewResponse());

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"s1\",\"courseId\":\"c1\",\"stars\":5}"))
                .andExpect(status().isCreated());
    }
}
