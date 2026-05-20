package ro.fiismart.dashboard.student.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.fiismart.dashboard.student.dto.StudentCourseHeaderDTO;
import ro.fiismart.dashboard.student.service.StudentCourseService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentCourseControllerTest {

    @Mock private StudentCourseService studentCourseService;
    @InjectMocks private StudentCourseController studentCourseController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studentCourseController).build();
    }

    @Test
    void getCourseHeader_returnsOk() throws Exception {
        StudentCourseHeaderDTO dto = new StudentCourseHeaderDTO();
        when(studentCourseService.getHeader("s1", "c1")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/students/s1/courses/c1"))
                .andExpect(status().isOk());
    }
}
