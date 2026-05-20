package ro.fiismart.quizattempt.controller;

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
import ro.fiismart.quizattempt.dto.QuizAttemptRequest;
import ro.fiismart.quizattempt.dto.QuizAttemptResponse;
import ro.fiismart.quizattempt.service.QuizAttemptService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptControllerTest {

    @Mock private QuizAttemptService quizAttemptService;
    @InjectMocks private QuizAttemptController quizAttemptController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(quizAttemptController).build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        QuizAttemptRequest req = new QuizAttemptRequest();
        req.setStudentId("s1");
        req.setQuizId("q1");
        req.setCourseId("c1");
        QuizAttemptResponse resp = QuizAttemptResponse.builder().id("a1").score(80).build();
        when(quizAttemptService.create(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/quiz-attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("a1"));
    }

    @Test
    void findById_returnsOk() throws Exception {
        when(quizAttemptService.findById("a1")).thenReturn(QuizAttemptResponse.builder().id("a1").build());

        mockMvc.perform(get("/api/v1/quiz-attempts/a1"))
                .andExpect(status().isOk());
    }

    @Test
    void findByStudent_returnsList() throws Exception {
        when(quizAttemptService.findByStudentId("s1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/quiz-attempts/student/s1"))
                .andExpect(status().isOk());
    }

    @Test
    void findByQuiz_returnsList() throws Exception {
        when(quizAttemptService.findByQuizId("q1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/quiz-attempts/quiz/q1"))
                .andExpect(status().isOk());
    }

    @Test
    void findByStudentAndQuiz_returnsList() throws Exception {
        when(quizAttemptService.findByStudentAndQuiz("s1", "q1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/quiz-attempts/student/s1/quiz/q1"))
                .andExpect(status().isOk());
    }

    @Test
    void findLatest_returnsOk() throws Exception {
        when(quizAttemptService.findLatestAttempt("s1", "q1"))
                .thenReturn(QuizAttemptResponse.builder().id("a1").build());

        mockMvc.perform(get("/api/v1/quiz-attempts/student/s1/quiz/q1/latest"))
                .andExpect(status().isOk());
    }

    @Test
    void findPassedByQuiz_returnsList() throws Exception {
        when(quizAttemptService.findPassedByQuiz("q1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/quiz-attempts/quiz/q1/passed"))
                .andExpect(status().isOk());
    }

    @Test
    void computeAvgScore_returnsMap() throws Exception {
        when(quizAttemptService.computeAvgScore("q1")).thenReturn(75.5);

        mockMvc.perform(get("/api/v1/quiz-attempts/quiz/q1/avg-score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgScore").value(75.5));
    }

    @Test
    void hasStudentPassed_returnsMap() throws Exception {
        when(quizAttemptService.hasStudentPassedQuiz("s1", "q1")).thenReturn(true);

        mockMvc.perform(get("/api/v1/quiz-attempts/student/s1/quiz/q1/passed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true));
    }

    @Test
    void countPassedByQuiz_returnsMap() throws Exception {
        when(quizAttemptService.countPassedByQuiz("q1")).thenReturn(10L);

        mockMvc.perform(get("/api/v1/quiz-attempts/count/quiz/q1/passed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(10));
    }

    @Test
    void deleteById_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/quiz-attempts/a1"))
                .andExpect(status().isNoContent());

        verify(quizAttemptService).deleteById("a1");
    }
}
