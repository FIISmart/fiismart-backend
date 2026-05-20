package ro.fiismart.dashboard.student.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.fiismart.dashboard.student.dto.StudentPlayableQuizDTO;
import ro.fiismart.dashboard.student.service.StudentQuizService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentPlayableQuizControllerTest {

    @Mock private StudentQuizService studentQuizService;
    @InjectMocks private StudentPlayableQuizController studentPlayableQuizController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studentPlayableQuizController).build();
    }

    @Test
    void getPlayableQuiz_returnsOk() throws Exception {
        StudentPlayableQuizDTO dto = StudentPlayableQuizDTO.builder().id("q1").build();
        when(studentQuizService.getPlayableQuiz("q1")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/student-quizzes/q1"))
                .andExpect(status().isOk());
    }
}
