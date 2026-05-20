package ro.fiismart.review.controller;

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
import ro.fiismart.review.dto.ReviewRequest;
import ro.fiismart.review.dto.ReviewResponse;
import ro.fiismart.review.service.ReviewService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock private ReviewService reviewService;
    @InjectMocks private ReviewController reviewController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController).build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        ReviewRequest req = new ReviewRequest();
        req.setStudentId("s1");
        req.setCourseId("c1");
        req.setStars(5);
        ReviewResponse resp = ReviewResponse.builder().id("r1").stars(5).build();
        when(reviewService.create(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("r1"));
    }

    @Test
    void findById_returnsOk() throws Exception {
        when(reviewService.findById("r1")).thenReturn(ReviewResponse.builder().id("r1").build());

        mockMvc.perform(get("/api/v1/reviews/r1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("r1"));
    }

    @Test
    void findByStudentAndCourse_returnsOk() throws Exception {
        when(reviewService.findByStudentAndCourse("s1", "c1"))
                .thenReturn(ReviewResponse.builder().id("r1").build());

        mockMvc.perform(get("/api/v1/reviews/student/s1/course/c1"))
                .andExpect(status().isOk());
    }

    @Test
    void findByCourse_returnsList() throws Exception {
        when(reviewService.findByCourseId("c1")).thenReturn(List.of(ReviewResponse.builder().id("r1").build()));

        mockMvc.perform(get("/api/v1/reviews/course/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("r1"));
    }

    @Test
    void findByStudent_returnsList() throws Exception {
        when(reviewService.findByStudentId("s1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reviews/student/s1"))
                .andExpect(status().isOk());
    }

    @Test
    void findByCourseAndStars_returnsList() throws Exception {
        when(reviewService.findByCourseAndStars("c1", 5)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reviews/course/c1/stars/5"))
                .andExpect(status().isOk());
    }

    @Test
    void computeAvgRating_returnsMap() throws Exception {
        when(reviewService.computeAvgRating("c1")).thenReturn(4.3);

        mockMvc.perform(get("/api/v1/reviews/course/c1/avg-rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgRating").value(4.3));
    }

    @Test
    void updateReview_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/reviews/r1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stars\":4,\"body\":\"Bun\"}"))
                .andExpect(status().isNoContent());

        verify(reviewService).updateReview("r1", 4, "Bun");
    }

    @Test
    void softDelete_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/reviews/r1/soft-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deletedBy\":\"admin\"}"))
                .andExpect(status().isNoContent());

        verify(reviewService).softDelete("r1", "admin");
    }

    @Test
    void deleteById_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/reviews/r1"))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteById("r1");
    }

    @Test
    void countByCourse_returnsMap() throws Exception {
        when(reviewService.countByCourse("c1")).thenReturn(7L);

        mockMvc.perform(get("/api/v1/reviews/count/course/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }

    @Test
    void hasReviewed_returnsMap() throws Exception {
        when(reviewService.hasStudentReviewedCourse("s1", "c1")).thenReturn(true);

        mockMvc.perform(get("/api/v1/reviews/exists/student/s1/course/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewed").value(true));
    }
}
