package com.app.controller;

import com.app.dto.request.CommentRequest;
import com.app.dto.response.CommentResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.ModerationFlag;
import com.app.service.CommentService;
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

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private CommentResponse buildCommentResponse() {
        return CommentResponse.builder()
                .id("cm1")
                .lectureId("lec1")
                .courseId("c1")
                .authorId("u1")
                .body("Great lecture!")
                .createdAt(new Date())
                .updatedAt(new Date())
                .deleted(false)
                .likeCount(0)
                .flagCount(0)
                .likedBy(new ArrayList<>())
                .moderationFlags(new ArrayList<>())
                .build();
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(commentService.create(any())).thenReturn(buildCommentResponse());

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CommentRequest.builder()
                                .lectureId("lec1")
                                .courseId("c1")
                                .authorId("u1")
                                .body("Great lecture!")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("cm1"))
                .andExpect(jsonPath("$.lectureId").value("lec1"))
                .andExpect(jsonPath("$.courseId").value("c1"))
                .andExpect(jsonPath("$.authorId").value("u1"))
                .andExpect(jsonPath("$.body").value("Great lecture!"));
    }

    @Test
    void create_withParentCommentId_returns201() throws Exception {
        CommentResponse reply = buildCommentResponse();
        reply.setParentCommentId("cm0");
        when(commentService.create(any())).thenReturn(reply);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CommentRequest.builder()
                                .lectureId("lec1")
                                .courseId("c1")
                                .authorId("u1")
                                .body("Reply!")
                                .parentCommentId("cm0")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentCommentId").value("cm0"));
    }

    @Test
    void create_blankLectureId_returns400() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lectureId\":\"\",\"courseId\":\"c1\",\"authorId\":\"u1\",\"body\":\"Body\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lectureId").exists());
    }

    @Test
    void create_blankCourseId_returns400() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lectureId\":\"lec1\",\"courseId\":\"\",\"authorId\":\"u1\",\"body\":\"Body\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.courseId").exists());
    }

    @Test
    void create_blankAuthorId_returns400() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lectureId\":\"lec1\",\"courseId\":\"c1\",\"authorId\":\"\",\"body\":\"Body\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.authorId").exists());
    }

    @Test
    void create_blankBody_returns400() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lectureId\":\"lec1\",\"courseId\":\"c1\",\"authorId\":\"u1\",\"body\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").exists());
    }

    @Test
    void create_serviceException_returns500() throws Exception {
        when(commentService.create(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CommentRequest.builder()
                                .lectureId("lec1")
                                .courseId("c1")
                                .authorId("u1")
                                .body("Body")
                                .build())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void findById_exists_returns200() throws Exception {
        when(commentService.findById("cm1")).thenReturn(buildCommentResponse());

        mockMvc.perform(get("/api/comments/{id}", "cm1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cm1"))
                .andExpect(jsonPath("$.body").value("Great lecture!"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(commentService.findById("missing")).thenThrow(new ResourceNotFoundException("Comment", "missing"));

        mockMvc.perform(get("/api/comments/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findByLecture_returns200() throws Exception {
        when(commentService.findByLectureId("lec1")).thenReturn(List.of(buildCommentResponse()));

        mockMvc.perform(get("/api/comments/lecture/{lectureId}", "lec1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].lectureId").value("lec1"));
    }

    @Test
    void findByLecture_emptyList_returns200() throws Exception {
        when(commentService.findByLectureId("lec99")).thenReturn(List.of());

        mockMvc.perform(get("/api/comments/lecture/{lectureId}", "lec99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findTopLevel_returns200() throws Exception {
        when(commentService.findTopLevelByLectureId("lec1")).thenReturn(List.of(buildCommentResponse()));

        mockMvc.perform(get("/api/comments/lecture/{lectureId}/top-level", "lec1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("cm1"));
    }

    @Test
    void findReplies_returns200() throws Exception {
        CommentResponse reply = buildCommentResponse();
        reply.setParentCommentId("cm0");
        when(commentService.findRepliesByParentId("cm0")).thenReturn(List.of(reply));

        mockMvc.perform(get("/api/comments/{parentId}/replies", "cm0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parentCommentId").value("cm0"));
    }

    @Test
    void findByAuthor_returns200() throws Exception {
        when(commentService.findByAuthorId("u1")).thenReturn(List.of(buildCommentResponse()));

        mockMvc.perform(get("/api/comments/author/{authorId}", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorId").value("u1"));
    }

    @Test
    void findFlagged_returns200() throws Exception {
        CommentResponse flagged = buildCommentResponse();
        flagged.setFlagCount(3);
        when(commentService.findFlagged(3)).thenReturn(List.of(flagged));

        mockMvc.perform(get("/api/comments/flagged/{minFlags}", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flagCount").value(3));
    }

    @Test
    void findFlagged_emptyList_returns200() throws Exception {
        when(commentService.findFlagged(100)).thenReturn(List.of());

        mockMvc.perform(get("/api/comments/flagged/{minFlags}", 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void updateBody_returns204() throws Exception {
        doNothing().when(commentService).updateBody(anyString(), anyString());

        mockMvc.perform(patch("/api/comments/{id}/body", "cm1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Updated body\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void softDelete_returns204() throws Exception {
        doNothing().when(commentService).softDelete(anyString());

        mockMvc.perform(patch("/api/comments/{id}/soft-delete", "cm1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void addLike_returns204() throws Exception {
        doNothing().when(commentService).addLike(anyString(), anyString());

        mockMvc.perform(patch("/api/comments/{id}/like/{userId}", "cm1", "u1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeLike_returns204() throws Exception {
        doNothing().when(commentService).removeLike(anyString(), anyString());

        mockMvc.perform(delete("/api/comments/{id}/like/{userId}", "cm1", "u1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void addFlag_returns204() throws Exception {
        doNothing().when(commentService).addModerationFlag(anyString(), any(ModerationFlag.class));

        ModerationFlag flag = ModerationFlag.builder()
                .flaggedBy("mod1")
                .reason("Inappropriate content")
                .flaggedAt(new Date())
                .build();

        mockMvc.perform(post("/api/comments/{id}/flags", "cm1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flag)))
                .andExpect(status().isNoContent());
    }

    @Test
    void clearFlags_returns204() throws Exception {
        doNothing().when(commentService).clearModerationFlags(anyString());

        mockMvc.perform(delete("/api/comments/{id}/flags", "cm1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_returns204() throws Exception {
        doNothing().when(commentService).deleteById(anyString());

        mockMvc.perform(delete("/api/comments/{id}", "cm1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void countByLecture_returns200() throws Exception {
        when(commentService.countByLecture("lec1")).thenReturn(7L);

        mockMvc.perform(get("/api/comments/count/lecture/{lectureId}", "lec1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }

    @Test
    void countByLecture_returnsZero() throws Exception {
        when(commentService.countByLecture("lec99")).thenReturn(0L);

        mockMvc.perform(get("/api/comments/count/lecture/{lectureId}", "lec99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void hasUserLiked_returnsTrue() throws Exception {
        when(commentService.hasUserLiked("cm1", "u1")).thenReturn(true);

        mockMvc.perform(get("/api/comments/{id}/liked-by/{userId}", "cm1", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));
    }

    @Test
    void hasUserLiked_returnsFalse() throws Exception {
        when(commentService.hasUserLiked("cm1", "u99")).thenReturn(false);

        mockMvc.perform(get("/api/comments/{id}/liked-by/{userId}", "cm1", "u99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false));
    }

    @Test
    void findById_verifyAllFields() throws Exception {
        CommentResponse response = CommentResponse.builder()
                .id("cm1")
                .lectureId("lec1")
                .courseId("c1")
                .authorId("u1")
                .body("Hello")
                .createdAt(new Date())
                .updatedAt(new Date())
                .deleted(false)
                .parentCommentId(null)
                .likeCount(5)
                .likedBy(List.of("u2", "u3"))
                .moderationFlags(new ArrayList<>())
                .flagCount(0)
                .build();

        when(commentService.findById("cm1")).thenReturn(response);

        mockMvc.perform(get("/api/comments/{id}", "cm1"))
                .andExpect(jsonPath("$.likeCount").value(5))
                .andExpect(jsonPath("$.flagCount").value(0))
                .andExpect(jsonPath("$.deleted").value(false))
                .andExpect(jsonPath("$.likedBy").isArray())
                .andExpect(jsonPath("$.moderationFlags").isArray());
    }

    @Test
    void findById_deletedComment_returns200() throws Exception {
        CommentResponse deleted = buildCommentResponse();
        deleted.setDeleted(true);
        when(commentService.findById("cm1")).thenReturn(deleted);

        mockMvc.perform(get("/api/comments/{id}", "cm1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }
}
