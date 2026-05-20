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
import ro.fiismart.quiz.dto.modulequiz.CreateModuleQuizRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizQuestionRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizResponse;
import ro.fiismart.quiz.service.ModuleQuizService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ModuleQuizControllerTest {

    @Mock private ModuleQuizService moduleQuizService;
    @InjectMocks private ModuleQuizController moduleQuizController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(moduleQuizController).build();
    }

    private ModuleQuizResponse sampleResp() {
        return ModuleQuizResponse.builder().id("mq1").title("Quiz").build();
    }

    @Test
    void getAllQuizzes_returnsOk() throws Exception {
        when(moduleQuizService.getAllQuizzesByCourse("c1")).thenReturn(List.of(sampleResp()));

        mockMvc.perform(get("/api/v1/courses/c1/builder/quizzes"))
                .andExpect(status().isOk());
    }

    @Test
    void getLectureQuiz_returnsOk() throws Exception {
        when(moduleQuizService.getLectureQuiz("c1", "m1", "l1")).thenReturn(sampleResp());

        mockMvc.perform(get("/api/v1/courses/c1/builder/modules/m1/lectures/l1/quiz"))
                .andExpect(status().isOk());
    }

    @Test
    void createLectureQuiz_returnsCreated() throws Exception {
        CreateModuleQuizRequest req = new CreateModuleQuizRequest();
        req.setTitle("Quiz L");
        when(moduleQuizService.createOrUpdateLectureQuiz(eq("c1"), eq("m1"), eq("l1"), any()))
                .thenReturn(sampleResp());

        mockMvc.perform(post("/api/v1/courses/c1/builder/modules/m1/lectures/l1/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteLectureQuiz_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/c1/builder/modules/m1/lectures/l1/quiz"))
                .andExpect(status().isNoContent());

        verify(moduleQuizService).deleteLectureQuiz("c1", "m1", "l1");
    }

    @Test
    void addQuestionToLectureQuiz_returnsCreated() throws Exception {
        ModuleQuizQuestionRequest req = new ModuleQuizQuestionRequest();
        req.setText("Întrebare?");
        when(moduleQuizService.addQuestionToLectureQuiz(eq("c1"), eq("m1"), eq("l1"), any()))
                .thenReturn(sampleResp());

        mockMvc.perform(post("/api/v1/courses/c1/builder/modules/m1/lectures/l1/quiz/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void removeQuestionFromLectureQuiz_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/c1/builder/modules/m1/lectures/l1/quiz/questions/qq1"))
                .andExpect(status().isNoContent());

        verify(moduleQuizService).removeQuestionFromLectureQuiz("c1", "m1", "l1", "qq1");
    }

    @Test
    void reorderLectureQuizQuestions_returnsOk() throws Exception {
        when(moduleQuizService.reorderLectureQuizQuestions(eq("c1"), eq("m1"), eq("l1"), any()))
                .thenReturn(sampleResp());

        mockMvc.perform(put("/api/v1/courses/c1/builder/modules/m1/lectures/l1/quiz/questions/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"qq1\",\"qq2\"]"))
                .andExpect(status().isOk());
    }

    @Test
    void getModuleQuiz_returnsOk() throws Exception {
        when(moduleQuizService.getModuleQuiz("c1", "m1")).thenReturn(sampleResp());

        mockMvc.perform(get("/api/v1/courses/c1/builder/modules/m1/quiz"))
                .andExpect(status().isOk());
    }

    @Test
    void createModuleQuiz_returnsCreated() throws Exception {
        CreateModuleQuizRequest req = new CreateModuleQuizRequest();
        req.setTitle("Quiz M");
        when(moduleQuizService.createOrUpdateModuleQuiz(eq("c1"), eq("m1"), any()))
                .thenReturn(sampleResp());

        mockMvc.perform(post("/api/v1/courses/c1/builder/modules/m1/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteModuleQuiz_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/c1/builder/modules/m1/quiz"))
                .andExpect(status().isNoContent());

        verify(moduleQuizService).deleteModuleQuiz("c1", "m1");
    }

    @Test
    void addQuestionToModuleQuiz_returnsCreated() throws Exception {
        ModuleQuizQuestionRequest req = new ModuleQuizQuestionRequest();
        req.setText("Q?");
        when(moduleQuizService.addQuestionToModuleQuiz(eq("c1"), eq("m1"), any()))
                .thenReturn(sampleResp());

        mockMvc.perform(post("/api/v1/courses/c1/builder/modules/m1/quiz/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void removeQuestionFromModuleQuiz_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/c1/builder/modules/m1/quiz/questions/qq1"))
                .andExpect(status().isNoContent());

        verify(moduleQuizService).removeQuestionFromModuleQuiz("c1", "m1", "qq1");
    }

    @Test
    void reorderModuleQuizQuestions_returnsOk() throws Exception {
        when(moduleQuizService.reorderModuleQuizQuestions(eq("c1"), eq("m1"), any()))
                .thenReturn(sampleResp());

        mockMvc.perform(put("/api/v1/courses/c1/builder/modules/m1/quiz/questions/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"qq1\"]"))
                .andExpect(status().isOk());
    }

    @Test
    void getCourseFinalQuiz_returnsOk() throws Exception {
        when(moduleQuizService.getCourseFinalQuiz("c1")).thenReturn(sampleResp());

        mockMvc.perform(get("/api/v1/courses/c1/builder/final-quiz"))
                .andExpect(status().isOk());
    }

    @Test
    void createCourseFinalQuiz_returnsCreated() throws Exception {
        CreateModuleQuizRequest req = new CreateModuleQuizRequest();
        req.setTitle("Final");
        when(moduleQuizService.createOrUpdateCourseFinalQuiz(eq("c1"), any()))
                .thenReturn(sampleResp());

        mockMvc.perform(post("/api/v1/courses/c1/builder/final-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteCourseFinalQuiz_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/c1/builder/final-quiz"))
                .andExpect(status().isNoContent());

        verify(moduleQuizService).deleteCourseFinalQuiz("c1");
    }

    @Test
    void addQuestionToCourseFinalQuiz_returnsCreated() throws Exception {
        ModuleQuizQuestionRequest req = new ModuleQuizQuestionRequest();
        req.setText("Q Final?");
        when(moduleQuizService.addQuestionToCourseFinalQuiz(eq("c1"), any()))
                .thenReturn(sampleResp());

        mockMvc.perform(post("/api/v1/courses/c1/builder/final-quiz/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void removeQuestionFromCourseFinalQuiz_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/c1/builder/final-quiz/questions/qq1"))
                .andExpect(status().isNoContent());

        verify(moduleQuizService).removeQuestionFromCourseFinalQuiz("c1", "qq1");
    }

    @Test
    void reorderCourseFinalQuizQuestions_returnsOk() throws Exception {
        when(moduleQuizService.reorderCourseFinalQuizQuestions(eq("c1"), any()))
                .thenReturn(sampleResp());

        mockMvc.perform(put("/api/v1/courses/c1/builder/final-quiz/questions/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"qq1\"]"))
                .andExpect(status().isOk());
    }
}
