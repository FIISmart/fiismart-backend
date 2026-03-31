package com.app.controller;

import com.app.dto.request.QuizAttemptRequest;
import com.app.dto.response.QuizAttemptResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Answer;
import com.app.service.QuizAttemptService;
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

@WebMvcTest(QuizAttemptController.class)
class QuizAttemptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuizAttemptService quizAttemptService;

    @Autowired
    private ObjectMapper objectMapper;

    private QuizAttemptResponse buildAttemptResponse() {
        return QuizAttemptResponse.builder()
                .id("a1")
                .quizId("q1")
                .courseId("c1")
                .studentId("s1")
                .attemptedAt(new Date())
                .score(85)
                .passed(true)
                .timeTakenSecs(300)
                .answers(new ArrayList<>())
                .build();
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(quizAttemptService.create(any())).thenReturn(buildAttemptResponse());

        mockMvc.perform(post("/api/quiz-attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(QuizAttemptRequest.builder()
                                .quizId("q1")
                                .courseId("c1")
                                .studentId("s1")
                                .score(85)
                                .passed(true)
                                .timeTakenSecs(300)
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("a1"))
                .andExpect(jsonPath("$.quizId").value("q1"))
                .andExpect(jsonPath("$.studentId").value("s1"))
                .andExpect(jsonPath("$.score").value(85))
                .andExpect(jsonPath("$.passed").value(true));
    }

    @Test
    void create_blankQuizId_returns400() throws Exception {
        mockMvc.perform(post("/api/quiz-attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":\"\",\"courseId\":\"c1\",\"studentId\":\"s1\",\"score\":80,\"timeTakenSecs\":200}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quizId").exists());
    }

    @Test
    void create_blankCourseId_returns400() throws Exception {
        mockMvc.perform(post("/api/quiz-attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":\"q1\",\"courseId\":\"\",\"studentId\":\"s1\",\"score\":80,\"timeTakenSecs\":200}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.courseId").exists());
    }

    @Test
    void create_blankStudentId_returns400() throws Exception {
        mockMvc.perform(post("/api/quiz-attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":\"q1\",\"courseId\":\"c1\",\"studentId\":\"\",\"score\":80,\"timeTakenSecs\":200}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentId").exists());
    }

    @Test
    void create_negativeScore_returns400() throws Exception {
        mockMvc.perform(post("/api/quiz-attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":\"q1\",\"courseId\":\"c1\",\"studentId\":\"s1\",\"score\":-1,\"timeTakenSecs\":200}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_negativeTimeTaken_returns400() throws Exception {
        mockMvc.perform(post("/api/quiz-attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":\"q1\",\"courseId\":\"c1\",\"studentId\":\"s1\",\"score\":80,\"timeTakenSecs\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_serviceException_returns500() throws Exception {
        when(quizAttemptService.create(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/quiz-attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(QuizAttemptRequest.builder()
                                .quizId("q1")
                                .courseId("c1")
                                .studentId("s1")
                                .score(80)
                                .timeTakenSecs(200)
                                .build())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void findById_exists_returns200() throws Exception {
        when(quizAttemptService.findById("a1")).thenReturn(buildAttemptResponse());

        mockMvc.perform(get("/api/quiz-attempts/{id}", "a1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("a1"))
                .andExpect(jsonPath("$.score").value(85));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(quizAttemptService.findById("missing")).thenThrow(new ResourceNotFoundException("QuizAttempt", "missing"));

        mockMvc.perform(get("/api/quiz-attempts/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findByStudent_returns200() throws Exception {
        when(quizAttemptService.findByStudentId("s1")).thenReturn(List.of(buildAttemptResponse()));

        mockMvc.perform(get("/api/quiz-attempts/student/{studentId}", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].studentId").value("s1"));
    }

    @Test
    void findByStudent_emptyList_returns200() throws Exception {
        when(quizAttemptService.findByStudentId("s99")).thenReturn(List.of());

        mockMvc.perform(get("/api/quiz-attempts/student/{studentId}", "s99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findByQuiz_returns200() throws Exception {
        when(quizAttemptService.findByQuizId("q1")).thenReturn(List.of(buildAttemptResponse()));

        mockMvc.perform(get("/api/quiz-attempts/quiz/{quizId}", "q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quizId").value("q1"));
    }

    @Test
    void findByStudentAndQuiz_returns200() throws Exception {
        when(quizAttemptService.findByStudentAndQuiz("s1", "q1")).thenReturn(List.of(buildAttemptResponse()));

        mockMvc.perform(get("/api/quiz-attempts/student/{studentId}/quiz/{quizId}", "s1", "q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].studentId").value("s1"));
    }

    @Test
    void findByStudentAndQuiz_emptyList_returns200() throws Exception {
        when(quizAttemptService.findByStudentAndQuiz("s99", "q99")).thenReturn(List.of());

        mockMvc.perform(get("/api/quiz-attempts/student/{studentId}/quiz/{quizId}", "s99", "q99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findLatest_exists_returns200() throws Exception {
        when(quizAttemptService.findLatestAttempt("s1", "q1")).thenReturn(buildAttemptResponse());

        mockMvc.perform(get("/api/quiz-attempts/student/{studentId}/quiz/{quizId}/latest", "s1", "q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("a1"));
    }

    @Test
    void findLatest_notFound_returns404() throws Exception {
        when(quizAttemptService.findLatestAttempt("s1", "q99"))
                .thenThrow(new ResourceNotFoundException("No attempt found for student s1 on quiz q99"));

        mockMvc.perform(get("/api/quiz-attempts/student/{studentId}/quiz/{quizId}/latest", "s1", "q99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findPassedByQuiz_returns200() throws Exception {
        when(quizAttemptService.findPassedByQuiz("q1")).thenReturn(List.of(buildAttemptResponse()));

        mockMvc.perform(get("/api/quiz-attempts/quiz/{quizId}/passed", "q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passed").value(true));
    }

    @Test
    void findPassedByQuiz_emptyList_returns200() throws Exception {
        when(quizAttemptService.findPassedByQuiz("q99")).thenReturn(List.of());

        mockMvc.perform(get("/api/quiz-attempts/quiz/{quizId}/passed", "q99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void computeAvgScore_returns200() throws Exception {
        when(quizAttemptService.computeAvgScore("q1")).thenReturn(82.5);

        mockMvc.perform(get("/api/quiz-attempts/quiz/{quizId}/avg-score", "q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgScore").value(82.5));
    }

    @Test
    void computeAvgScore_zero_returns200() throws Exception {
        when(quizAttemptService.computeAvgScore("q99")).thenReturn(0.0);

        mockMvc.perform(get("/api/quiz-attempts/quiz/{quizId}/avg-score", "q99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgScore").value(0.0));
    }

    @Test
    void hasStudentPassed_returnsTrue() throws Exception {
        when(quizAttemptService.hasStudentPassedQuiz("s1", "q1")).thenReturn(true);

        mockMvc.perform(get("/api/quiz-attempts/student/{studentId}/quiz/{quizId}/passed", "s1", "q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true));
    }

    @Test
    void hasStudentPassed_returnsFalse() throws Exception {
        when(quizAttemptService.hasStudentPassedQuiz("s1", "q2")).thenReturn(false);

        mockMvc.perform(get("/api/quiz-attempts/student/{studentId}/quiz/{quizId}/passed", "s1", "q2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(false));
    }

    @Test
    void countPassedByQuiz_returns200() throws Exception {
        when(quizAttemptService.countPassedByQuiz("q1")).thenReturn(15L);

        mockMvc.perform(get("/api/quiz-attempts/count/quiz/{quizId}/passed", "q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(15));
    }

    @Test
    void countPassedByQuiz_zero_returns200() throws Exception {
        when(quizAttemptService.countPassedByQuiz("q99")).thenReturn(0L);

        mockMvc.perform(get("/api/quiz-attempts/count/quiz/{quizId}/passed", "q99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void deleteById_returns204() throws Exception {
        doNothing().when(quizAttemptService).deleteById(anyString());

        mockMvc.perform(delete("/api/quiz-attempts/{id}", "a1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_serviceException_returns500() throws Exception {
        doThrow(new RuntimeException("DB error")).when(quizAttemptService).deleteById("error");

        mockMvc.perform(delete("/api/quiz-attempts/{id}", "error"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void create_withAnswers_returns201() throws Exception {
        Answer answer = Answer.builder().questionId("qq1").selectedIdx(0).correct(true).build();
        QuizAttemptResponse response = buildAttemptResponse();
        response.setAnswers(List.of(answer));
        when(quizAttemptService.create(any())).thenReturn(response);

        QuizAttemptRequest request = QuizAttemptRequest.builder()
                .quizId("q1")
                .courseId("c1")
                .studentId("s1")
                .score(100)
                .passed(true)
                .timeTakenSecs(120)
                .answers(List.of(answer))
                .build();

        mockMvc.perform(post("/api/quiz-attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answers").isArray())
                .andExpect(jsonPath("$.answers[0].questionId").value("qq1"));
    }

    @Test
    void findById_verifyAllFields() throws Exception {
        QuizAttemptResponse response = QuizAttemptResponse.builder()
                .id("a1")
                .quizId("q1")
                .courseId("c1")
                .studentId("s1")
                .attemptedAt(new Date())
                .score(90)
                .passed(true)
                .timeTakenSecs(500)
                .answers(new ArrayList<>())
                .build();

        when(quizAttemptService.findById("a1")).thenReturn(response);

        mockMvc.perform(get("/api/quiz-attempts/{id}", "a1"))
                .andExpect(jsonPath("$.quizId").value("q1"))
                .andExpect(jsonPath("$.courseId").value("c1"))
                .andExpect(jsonPath("$.timeTakenSecs").value(500))
                .andExpect(jsonPath("$.answers").isArray());
    }
}
