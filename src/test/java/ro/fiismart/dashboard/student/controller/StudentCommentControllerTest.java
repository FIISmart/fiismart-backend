package ro.fiismart.dashboard.student.controller;

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
import ro.fiismart.dashboard.student.dto.CommentCreateRequest;
import ro.fiismart.dashboard.student.dto.StudentCommentDTO;
import ro.fiismart.dashboard.student.service.StudentCommentService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudentCommentControllerTest {

    @Mock private StudentCommentService commentService;
    @InjectMocks private StudentCommentController studentCommentController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studentCommentController).build();
    }

    @Test
    void getComments_returnsOk() throws Exception {
        when(commentService.getCommentsThreaded("s1", "l1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/students/s1/courses/c1/lectures/l1/comments"))
                .andExpect(status().isOk());
    }

    @Test
    void createComment_returnsOk() throws Exception {
        CommentCreateRequest req = new CommentCreateRequest();
        req.setBody("Întrebare?");
        StudentCommentDTO dto = StudentCommentDTO.builder().build();
        when(commentService.createComment(eq("s1"), eq("c1"), eq("l1"), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/students/s1/courses/c1/lectures/l1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void replyToComment_returnsOk() throws Exception {
        CommentCreateRequest req = new CommentCreateRequest();
        req.setBody("Răspuns");
        StudentCommentDTO dto = StudentCommentDTO.builder().build();
        when(commentService.replyToComment(eq("s1"), eq("cm1"), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/students/s1/comments/cm1/replies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void toggleLike_returnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/students/s1/comments/cm1/like"))
                .andExpect(status().isOk());

        verify(commentService).toggleLike("s1", "cm1");
    }
}
