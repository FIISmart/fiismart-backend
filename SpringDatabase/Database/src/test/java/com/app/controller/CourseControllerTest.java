package com.app.controller;

import com.app.dto.request.CourseRequest;
import com.app.dto.request.LectureRequest;
import com.app.dto.response.CourseResponse;
import com.app.dto.response.LectureResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.service.CourseService;
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

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @Autowired
    private ObjectMapper objectMapper;

    private CourseResponse buildCourseResponse() {
        return CourseResponse.builder()
                .id("c1")
                .title("Java 101")
                .description("Learn Java")
                .teacherId("t1")
                .status("draft")
                .tags(List.of("java"))
                .enrollmentCount(0)
                .avgRating(0.0)
                .lectures(new ArrayList<>())
                .hidden(false)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
    }

    private LectureResponse buildLectureResponse() {
        return LectureResponse.builder()
                .id("lec1")
                .title("Intro")
                .videoUrl("http://video.com")
                .imageUrls(List.of())
                .order(1)
                .durationSecs(300)
                .publishedAt(new Date())
                .build();
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(courseService.create(any())).thenReturn(buildCourseResponse());

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CourseRequest.builder()
                                .title("Java 101")
                                .teacherId("t1")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("c1"))
                .andExpect(jsonPath("$.title").value("Java 101"))
                .andExpect(jsonPath("$.teacherId").value("t1"))
                .andExpect(jsonPath("$.status").value("draft"));
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"teacherId\":\"t1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void create_missingTeacherId_returns400() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Course\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.teacherId").exists());
    }

    @Test
    void create_serviceException_returns500() throws Exception {
        when(courseService.create(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CourseRequest.builder()
                                .title("Course")
                                .teacherId("t1")
                                .build())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void findById_exists_returns200() throws Exception {
        when(courseService.findById("c1")).thenReturn(buildCourseResponse());

        mockMvc.perform(get("/api/courses/{id}", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("c1"))
                .andExpect(jsonPath("$.title").value("Java 101"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(courseService.findById("missing")).thenThrow(new ResourceNotFoundException("Course", "missing"));

        mockMvc.perform(get("/api/courses/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void findById_serviceException_returns500() throws Exception {
        when(courseService.findById("error")).thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/courses/{id}", "error"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void findAll_returns200WithList() throws Exception {
        when(courseService.findAll()).thenReturn(List.of(buildCourseResponse()));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("c1"));
    }

    @Test
    void findAll_emptyList_returns200() throws Exception {
        when(courseService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findByTeacher_returns200() throws Exception {
        when(courseService.findByTeacherId("t1")).thenReturn(List.of(buildCourseResponse()));

        mockMvc.perform(get("/api/courses/teacher/{teacherId}", "t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teacherId").value("t1"));
    }

    @Test
    void findPublishedVisible_returns200() throws Exception {
        CourseResponse published = buildCourseResponse();
        published.setStatus("published");
        when(courseService.findPublishedVisible()).thenReturn(List.of(published));

        mockMvc.perform(get("/api/courses/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("published"));
    }

    @Test
    void findByTag_returns200() throws Exception {
        when(courseService.findByTag("java")).thenReturn(List.of(buildCourseResponse()));

        mockMvc.perform(get("/api/courses/tag/{tag}", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void findByMinRating_returns200() throws Exception {
        CourseResponse rated = buildCourseResponse();
        rated.setAvgRating(4.5);
        when(courseService.findByMinRating(4.0)).thenReturn(List.of(rated));

        mockMvc.perform(get("/api/courses/min-rating/{minRating}", 4.0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].avgRating").value(4.5));
    }

    @Test
    void findLectures_returns200WithLectures() throws Exception {
        when(courseService.findLecturesByCourseId("c1")).thenReturn(List.of(buildLectureResponse()));

        mockMvc.perform(get("/api/courses/{courseId}/lectures", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("lec1"))
                .andExpect(jsonPath("$[0].title").value("Intro"));
    }

    @Test
    void findLectures_emptyList_returns200() throws Exception {
        when(courseService.findLecturesByCourseId("c99")).thenReturn(List.of());

        mockMvc.perform(get("/api/courses/{courseId}/lectures", "c99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void updateTitle_returns204() throws Exception {
        doNothing().when(courseService).updateTitle(anyString(), anyString());

        mockMvc.perform(patch("/api/courses/{id}/title", "c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Title\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateStatus_returns204() throws Exception {
        doNothing().when(courseService).updateStatus(anyString(), anyString());

        mockMvc.perform(patch("/api/courses/{id}/status", "c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"published\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void setHidden_true_returns204() throws Exception {
        doNothing().when(courseService).setHidden(anyString(), anyBoolean());

        mockMvc.perform(patch("/api/courses/{id}/hidden", "c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hidden\":true}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void setHidden_false_returns204() throws Exception {
        doNothing().when(courseService).setHidden(anyString(), anyBoolean());

        mockMvc.perform(patch("/api/courses/{id}/hidden", "c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hidden\":false}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void setQuizId_returns204() throws Exception {
        doNothing().when(courseService).setQuizId(anyString(), anyString());

        mockMvc.perform(patch("/api/courses/{id}/quiz/{quizId}", "c1", "q1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void addLecture_validRequest_returns201() throws Exception {
        when(courseService.addLecture(eq("c1"), any())).thenReturn(buildLectureResponse());

        mockMvc.perform(post("/api/courses/{courseId}/lectures", "c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LectureRequest.builder()
                                .title("Lesson 1")
                                .videoUrl("http://v.com")
                                .order(1)
                                .durationSecs(300)
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("lec1"))
                .andExpect(jsonPath("$.title").value("Intro"));
    }

    @Test
    void addLecture_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/courses/{courseId}/lectures", "c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"order\":1,\"durationSecs\":300}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void addLecture_negativeOrder_returns400() throws Exception {
        mockMvc.perform(post("/api/courses/{courseId}/lectures", "c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lesson\",\"order\":-1,\"durationSecs\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeLecture_returns204() throws Exception {
        doNothing().when(courseService).removeLecture(anyString(), anyString());

        mockMvc.perform(delete("/api/courses/{courseId}/lectures/{lectureId}", "c1", "lec1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_returns204() throws Exception {
        doNothing().when(courseService).deleteById(anyString());

        mockMvc.perform(delete("/api/courses/{id}", "c1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void countByTeacher_returns200() throws Exception {
        when(courseService.countByTeacher("t1")).thenReturn(5L);

        mockMvc.perform(get("/api/courses/count/teacher/{teacherId}", "t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void countByTeacher_returnsZero() throws Exception {
        when(courseService.countByTeacher("t99")).thenReturn(0L);

        mockMvc.perform(get("/api/courses/count/teacher/{teacherId}", "t99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void findById_verifyJsonStructure() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id("c1")
                .title("Java")
                .description("Desc")
                .teacherId("t1")
                .status("published")
                .tags(List.of("java", "oop"))
                .enrollmentCount(10)
                .avgRating(4.5)
                .lectures(new ArrayList<>())
                .hidden(false)
                .quizId("q1")
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        when(courseService.findById("c1")).thenReturn(response);

        mockMvc.perform(get("/api/courses/{id}", "c1"))
                .andExpect(jsonPath("$.description").value("Desc"))
                .andExpect(jsonPath("$.enrollmentCount").value(10))
                .andExpect(jsonPath("$.avgRating").value(4.5))
                .andExpect(jsonPath("$.hidden").value(false))
                .andExpect(jsonPath("$.quizId").value("q1"))
                .andExpect(jsonPath("$.tags").isArray());
    }
}
