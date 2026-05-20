package ro.fiismart.notification.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.fiismart.notification.dto.NotificationResponse;
import ro.fiismart.notification.service.NotificationService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    @Test
    void getNotifications_returnsOk() throws Exception {
        when(notificationService.getForUser(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk());
    }

    @Test
    void getUnreadCount_returnsCount() throws Exception {
        when(notificationService.getUnreadCount(null)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void markRead_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/n1/read"))
                .andExpect(status().isNoContent());

        verify(notificationService).markRead("n1");
    }

    @Test
    void markAllRead_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllRead(null);
    }
}
