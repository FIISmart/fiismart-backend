package ro.fiismart.admin.controller;

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
import ro.fiismart.admin.dto.*;
import ro.fiismart.admin.service.AdminService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private AdminService adminService;
    @InjectMocks private AdminController adminController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    void getStats_returnsOk() throws Exception {
        when(adminService.getStats()).thenReturn(AdminStatsResponse.builder().build());

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_returnsOk() throws Exception {
        when(adminService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    void updateUser_returnsOk() throws Exception {
        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setDisplayName("Test User");
        AdminUserResponse resp = AdminUserResponse.builder().build();
        when(adminService.updateUser(eq("u1"), any())).thenReturn(resp);

        mockMvc.perform(put("/api/v1/admin/users/u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUser_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/u1"))
                .andExpect(status().isNoContent());

        verify(adminService).deleteUser("u1");
    }

    @Test
    void getAllCourses_returnsOk() throws Exception {
        when(adminService.getAllCourses()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/courses"))
                .andExpect(status().isOk());
    }

    @Test
    void updateCourse_returnsOk() throws Exception {
        AdminUpdateCourseRequest req = new AdminUpdateCourseRequest();
        AdminCourseResponse resp = AdminCourseResponse.builder().build();
        when(adminService.updateCourse(eq("c1"), any())).thenReturn(resp);

        mockMvc.perform(put("/api/v1/admin/courses/c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCourse_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/courses/c1"))
                .andExpect(status().isNoContent());

        verify(adminService).deleteCourse("c1");
    }

    @Test
    void getAllEnrollments_returnsOk() throws Exception {
        when(adminService.getAllEnrollments()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/enrollments"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteEnrollment_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/enrollments/e1"))
                .andExpect(status().isNoContent());

        verify(adminService).deleteEnrollment("e1");
    }
}
