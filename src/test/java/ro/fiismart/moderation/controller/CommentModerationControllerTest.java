package ro.fiismart.moderation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.fiismart.moderation.dto.CommentModerationResponse;
import ro.fiismart.moderation.service.CommentModerationService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CommentModerationControllerTest {

    @Mock private CommentModerationService commentModerationService;
    @InjectMocks private CommentModerationController commentModerationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentModerationController).build();
    }

    @Test
    void getComments_returnsOk() throws Exception {
        when(commentModerationService.getCommentsByCourseId("c1"))
                .thenReturn(List.of(CommentModerationResponse.builder().id("c1").build()));

        mockMvc.perform(get("/api/v1/courses/c1/comments"))
                .andExpect(status().isOk());
    }

    @Test
    void updateCommentStatus_returnsOk() throws Exception {
        mockMvc.perform(patch("/api/v1/comments/cm1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"approved\"}"))
                .andExpect(status().isOk());

        verify(commentModerationService).updateCommentStatus("cm1", "approved");
    }

    @Test
    void deleteComment_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/cm1"))
                .andExpect(status().isNoContent());

        verify(commentModerationService).deleteComment("cm1");
    }
}
