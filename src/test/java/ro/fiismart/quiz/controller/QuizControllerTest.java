package ro.fiismart.quiz.controller;

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
import ro.fiismart.quiz.dto.*;
import ro.fiismart.quiz.service.QuizManagementService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    @Mock private QuizManagementService quizService;
    @InjectMocks private QuizController quizController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(quizController).build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        QuizRequest req = new QuizRequest();
        req.setCourseId("c1");
        req.setTitle("Quiz");
        QuizResponse resp = QuizResponse.builder().id("q1").title("Quiz").build();
        when(quizService.create(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("q1"));
    }

    @Test
    void findById_returnsOk() throws Exception {
        when(quizService.findById("q1")).thenReturn(QuizResponse.builder().id("q1").build());

        mockMvc.perform(get("/api/v1/quizzes/q1"))
                .andExpect(status().isOk());
    }

    @Test
    void findByCourseId_returnsOk() throws Exception {
        when(quizService.findByCourseId("c1")).thenReturn(QuizResponse.builder().id("q1").build());

        mockMvc.perform(get("/api/v1/quizzes/course/c1"))
                .andExpect(status().isOk());
    }

    @Test
    void findQuestions_returnsList() throws Exception {
        when(quizService.findQuestions("q1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/quizzes/q1/questions"))
                .andExpect(status().isOk());
    }

    @Test
    void updateTitle_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/quizzes/q1/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nou\"}"))
                .andExpect(status().isNoContent());

        verify(quizService).updateTitle("q1", "Nou");
    }

    @Test
    void updatePassingScore_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/quizzes/q1/passing-score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passingScore\":70}"))
                .andExpect(status().isNoContent());

        verify(quizService).updatePassingScore("q1", 70);
    }

    @Test
    void updateTimeLimit_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/quizzes/q1/time-limit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timeLimit\":30}"))
                .andExpect(status().isNoContent());

        verify(quizService).updateTimeLimit("q1", 30);
    }

    @Test
    void addQuestion_returnsCreated() throws Exception {
        QuizQuestionRequest req = new QuizQuestionRequest();
        req.setText("Ce este Pi?");
        QuizQuestionResponse resp = QuizQuestionResponse.builder().id("qq1").text("Ce este Pi?").build();
        when(quizService.addQuestion(eq("q1"), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/quizzes/q1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void removeQuestion_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/quizzes/q1/questions/qq1"))
                .andExpect(status().isNoContent());

        verify(quizService).removeQuestion("q1", "qq1");
    }

    @Test
    void deleteById_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/quizzes/q1"))
                .andExpect(status().isNoContent());

        verify(quizService).deleteById("q1");
    }

    @Test
    void deleteByCourseId_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/quizzes/course/c1"))
                .andExpect(status().isNoContent());

        verify(quizService).deleteByCourseId("c1");
    }
}
