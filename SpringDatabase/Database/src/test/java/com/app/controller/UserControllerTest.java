package com.app.controller;

import com.app.dto.request.UserRequest;
import com.app.dto.response.UserResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserResponse buildResponse() {
        return UserResponse.builder()
                .id("u1")
                .displayName("Test User")
                .email("test@test.com")
                .role("student")
                .banned(false)
                .ownedCourses(List.of())
                .enrolledCourseIds(List.of())
                .build();
    }

    @Test
    void create_validRequest_returns201WithBody() throws Exception {
        when(userService.create(any())).thenReturn(buildResponse());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UserRequest.builder()
                                .displayName("Test User")
                                .email("test@test.com")
                                .role("student")
                                .password("secret123")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("u1"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.role").value("student"))
                .andExpect(jsonPath("$.banned").value(false));
    }

    @Test
    void create_blankDisplayName_returns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"\",\"email\":\"t@t.com\",\"role\":\"student\",\"password\":\"p\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void create_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Name\",\"role\":\"student\",\"password\":\"p\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void create_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Name\",\"email\":\"notanemail\",\"role\":\"student\",\"password\":\"p\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void create_blankRole_returns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Name\",\"email\":\"t@t.com\",\"role\":\"\",\"password\":\"p\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Name\",\"email\":\"t@t.com\",\"role\":\"student\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_serviceThrowsException_returns500() throws Exception {
        when(userService.create(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UserRequest.builder()
                                .displayName("Name")
                                .email("t@t.com")
                                .role("student")
                                .password("p")
                                .build())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void findById_exists_returns200WithBody() throws Exception {
        when(userService.findById("u1")).thenReturn(buildResponse());

        mockMvc.perform(get("/api/users/{id}", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u1"))
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(userService.findById("missing")).thenThrow(new ResourceNotFoundException("User", "missing"));

        mockMvc.perform(get("/api/users/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void findById_serviceException_returns500() throws Exception {
        when(userService.findById("error")).thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/users/{id}", "error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void findByEmail_exists_returns200() throws Exception {
        when(userService.findByEmail("test@test.com")).thenReturn(buildResponse());

        mockMvc.perform(get("/api/users/email/{email}", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    void findByEmail_notFound_returns404() throws Exception {
        when(userService.findByEmail("missing@test.com"))
                .thenThrow(new ResourceNotFoundException("User not found with email: missing@test.com"));

        mockMvc.perform(get("/api/users/email/{email}", "missing@test.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findByRole_returns200WithList() throws Exception {
        when(userService.findAllByRole("student")).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/users/role/{role}", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].role").value("student"));
    }

    @Test
    void findByRole_returnsEmptyList_returns200() throws Exception {
        when(userService.findAllByRole("admin")).thenReturn(List.of());

        mockMvc.perform(get("/api/users/role/{role}", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findAllTeachers_returns200() throws Exception {
        UserResponse teacher = UserResponse.builder()
                .id("t1").displayName("Teacher").email("t@t.com").role("teacher").build();
        when(userService.findAllTeachers()).thenReturn(List.of(teacher));

        mockMvc.perform(get("/api/users/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("teacher"));
    }

    @Test
    void findAllStudents_returns200() throws Exception {
        when(userService.findAllStudents()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/users/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void findBanned_returns200() throws Exception {
        UserResponse banned = UserResponse.builder()
                .id("b1").displayName("Banned").email("b@b.com").role("student").banned(true).build();
        when(userService.findBannedUsers()).thenReturn(List.of(banned));

        mockMvc.perform(get("/api/users/banned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].banned").value(true));
    }

    @Test
    void findStudentsEnrolledInCourse_returns200() throws Exception {
        when(userService.findStudentsEnrolledInCourse("c1")).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/users/enrolled-in/{courseId}", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateDisplayName_returns204() throws Exception {
        doNothing().when(userService).updateDisplayName(anyString(), anyString());

        mockMvc.perform(patch("/api/users/{id}/display-name", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"New Name\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void banUser_returns204() throws Exception {
        doNothing().when(userService).banUser(anyString(), anyString(), anyString());

        mockMvc.perform(patch("/api/users/{id}/ban", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bannedBy\":\"admin1\",\"reason\":\"Violation\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void unbanUser_returns204() throws Exception {
        doNothing().when(userService).unbanUser(anyString());

        mockMvc.perform(patch("/api/users/{id}/unban", "u1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void addOwnedCourse_returns204() throws Exception {
        doNothing().when(userService).addOwnedCourse(anyString(), anyString());

        mockMvc.perform(patch("/api/users/{teacherId}/owned-courses/{courseId}", "t1", "c1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeOwnedCourse_returns204() throws Exception {
        doNothing().when(userService).removeOwnedCourse(anyString(), anyString());

        mockMvc.perform(delete("/api/users/{teacherId}/owned-courses/{courseId}", "t1", "c1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void addEnrolledCourse_returns204() throws Exception {
        doNothing().when(userService).addEnrolledCourse(anyString(), anyString());

        mockMvc.perform(patch("/api/users/{studentId}/enrolled-courses/{courseId}", "s1", "c1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeEnrolledCourse_returns204() throws Exception {
        doNothing().when(userService).removeEnrolledCourse(anyString(), anyString());

        mockMvc.perform(delete("/api/users/{studentId}/enrolled-courses/{courseId}", "s1", "c1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_returns204() throws Exception {
        doNothing().when(userService).deleteById(anyString());

        mockMvc.perform(delete("/api/users/{id}", "u1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_serviceException_returns500() throws Exception {
        doThrow(new RuntimeException("DB error")).when(userService).deleteById("error");

        mockMvc.perform(delete("/api/users/{id}", "error"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void existsByEmail_returnsTrue() throws Exception {
        when(userService.existsByEmail("exists@test.com")).thenReturn(true);

        mockMvc.perform(get("/api/users/exists/email/{email}", "exists@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void existsByEmail_returnsFalse() throws Exception {
        when(userService.existsByEmail("nope@test.com")).thenReturn(false);

        mockMvc.perform(get("/api/users/exists/email/{email}", "nope@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    void countByRole_returns200WithCount() throws Exception {
        when(userService.countByRole("student")).thenReturn(42L);

        mockMvc.perform(get("/api/users/count/role/{role}", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(42));
    }

    @Test
    void countByRole_returnsZero() throws Exception {
        when(userService.countByRole("admin")).thenReturn(0L);

        mockMvc.perform(get("/api/users/count/role/{role}", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }
}
