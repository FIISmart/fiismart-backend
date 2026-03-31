package com.app.controller;

import com.app.dto.request.QuizQuestionRequest;
import com.app.dto.request.QuizRequest;
import com.app.dto.response.QuizQuestionResponse;
import com.app.dto.response.QuizResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.service.QuizService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuizService quizService;

    @Autowired
    private ObjectMapper objectMapper;

    private QuizResponse buildQuizResponse() {
        return QuizResponse.builder()
                .id("q1")
                .courseId("c1")
                .title("Final Quiz")
                .passingScore(70)
                .timeLimit(30)
                .shuffleQuestions(false)
                .questions(new ArrayList<>())
                .build();
    }

    private QuizQuestionResponse buildQuestionResponse() {
        return QuizQuestionResponse.builder()
                .id("qq1")
                .text("What is Java?")
                .type("single")
                .points(10)
                .options(List.of("A lang", "A drink"))
                .correctIdx(0)
                .explanation("Java is a programming language")
                .build();
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(quizService.create(any())).thenReturn(buildQuizResponse());

        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(QuizRequest.builder()
                                .courseId("c1")
                                .title("Final Quiz")
                                .passingScore(70)
                                .timeLimit(30)
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("q1"))
                .andExpect(jsonPath("$.courseId").value("c1"))
                .andExpect(jsonPath("$.title").value("Final Quiz"))
                .andExpect(jsonPath("$.passingScore").value(70))
                .andExpect(jsonPath("$.timeLimit").value(30));
    }

    @Test
    void create_blankCourseId_returns400() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"\",\"title\":\"Quiz\",\"passingScore\":70,\"timeLimit\":30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.courseId").exists());
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"c1\",\"title\":\"\",\"passingScore\":70,\"timeLimit\":30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void create_timeLimitZero_returns400() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"c1\",\"title\":\"Quiz\",\"passingScore\":70,\"timeLimit\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.timeLimit").exists());
    }

    @Test
    void create_negativePassingScore_returns400() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"c1\",\"title\":\"Quiz\",\"passingScore\":-1,\"timeLimit\":30}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_serviceException_returns500() throws Exception {
        when(quizService.create(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(QuizRequest.builder()
                                .courseId("c1")
                                .title("Quiz")
                                .passingScore(70)
                                .timeLimit(30)
                                .build())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void findById_exists_returns200() throws Exception {
        when(quizService.findById("q1")).thenReturn(buildQuizResponse());

        mockMvc.perform(get("/api/quizzes/{id}", "q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("q1"))
                .andExpect(jsonPath("$.title").value("Final Quiz"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(quizService.findById("missing")).thenThrow(new ResourceNotFoundException("Quiz", "missing"));

        mockMvc.perform(get("/api/quizzes/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findByCourseId_exists_returns200() throws Exception {
        when(quizService.findByCourseId("c1")).thenReturn(buildQuizResponse());

        mockMvc.perform(get("/api/quizzes/course/{courseId}", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value("c1"));
    }

    @Test
    void findByCourseId_notFound_returns404() throws Exception {
        when(quizService.findByCourseId("missing"))
                .thenThrow(new ResourceNotFoundException("Quiz not found for course: missing"));

        mockMvc.perform(get("/api/quizzes/course/{courseId}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findQuestions_returns200WithQuestions() throws Exception {
        when(quizService.findQuestions("q1")).thenReturn(List.of(buildQuestionResponse()));

        mockMvc.perform(get("/api/quizzes/{quizId}/questions", "q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("qq1"))
                .andExpect(jsonPath("$[0].text").value("What is Java?"))
                .andExpect(jsonPath("$[0].type").value("single"))
                .andExpect(jsonPath("$[0].points").value(10));
    }

    @Test
    void findQuestions_emptyList_returns200() throws Exception {
        when(quizService.findQuestions("q99")).thenReturn(List.of());

        mockMvc.perform(get("/api/quizzes/{quizId}/questions", "q99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void updateTitle_returns204() throws Exception {
        doNothing().when(quizService).updateTitle(anyString(), anyString());

        mockMvc.perform(patch("/api/quizzes/{id}/title", "q1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Title\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updatePassingScore_returns204() throws Exception {
        doNothing().when(quizService).updatePassingScore(anyString(), anyInt());

        mockMvc.perform(patch("/api/quizzes/{id}/passing-score", "q1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passingScore\":80}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateTimeLimit_returns204() throws Exception {
        doNothing().when(quizService).updateTimeLimit(anyString(), anyInt());

        mockMvc.perform(patch("/api/quizzes/{id}/time-limit", "q1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timeLimit\":45}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void addQuestion_validRequest_returns201() throws Exception {
        when(quizService.addQuestion(eq("q1"), any())).thenReturn(buildQuestionResponse());

        mockMvc.perform(post("/api/quizzes/{quizId}/questions", "q1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(QuizQuestionRequest.builder()
                                .text("What is Java?")
                                .type("single")
                                .points(10)
                                .options(List.of("A lang", "A drink"))
                                .correctIdx(0)
                                .explanation("A language")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("qq1"))
                .andExpect(jsonPath("$.text").value("What is Java?"))
                .andExpect(jsonPath("$.type").value("single"))
                .andExpect(jsonPath("$.points").value(10))
                .andExpect(jsonPath("$.correctIdx").value(0));
    }

    @Test
    void addQuestion_blankText_returns400() throws Exception {
        mockMvc.perform(post("/api/quizzes/{quizId}/questions", "q1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\",\"type\":\"single\",\"points\":5,\"options\":[\"A\"],\"correctIdx\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.text").exists());
    }

    @Test
    void addQuestion_blankType_returns400() throws Exception {
        mockMvc.perform(post("/api/quizzes/{quizId}/questions", "q1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Q?\",\"type\":\"\",\"points\":5,\"options\":[\"A\"],\"correctIdx\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").exists());
    }

    @Test
    void addQuestion_pointsZero_returns400() throws Exception {
        mockMvc.perform(post("/api/quizzes/{quizId}/questions", "q1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Q?\",\"type\":\"single\",\"points\":0,\"options\":[\"A\"],\"correctIdx\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.points").exists());
    }

    @Test
    void addQuestion_emptyOptions_returns400() throws Exception {
        mockMvc.perform(post("/api/quizzes/{quizId}/questions", "q1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Q?\",\"type\":\"single\",\"points\":5,\"options\":[],\"correctIdx\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.options").exists());
    }

    @Test
    void removeQuestion_returns204() throws Exception {
        doNothing().when(quizService).removeQuestion(anyString(), anyString());

        mockMvc.perform(delete("/api/quizzes/{quizId}/questions/{questionId}", "q1", "qq1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_returns204() throws Exception {
        doNothing().when(quizService).deleteById(anyString());

        mockMvc.perform(delete("/api/quizzes/{id}", "q1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteByCourseId_returns204() throws Exception {
        doNothing().when(quizService).deleteByCourseId(anyString());

        mockMvc.perform(delete("/api/quizzes/course/{courseId}", "c1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void findById_verifyAllJsonFields() throws Exception {
        QuizResponse response = QuizResponse.builder()
                .id("q1")
                .courseId("c1")
                .title("Quiz")
                .passingScore(75)
                .timeLimit(45)
                .shuffleQuestions(true)
                .questions(new ArrayList<>())
                .build();

        when(quizService.findById("q1")).thenReturn(response);

        mockMvc.perform(get("/api/quizzes/{id}", "q1"))
                .andExpect(jsonPath("$.passingScore").value(75))
                .andExpect(jsonPath("$.timeLimit").value(45))
                .andExpect(jsonPath("$.shuffleQuestions").value(true))
                .andExpect(jsonPath("$.questions").isArray());
    }
}
