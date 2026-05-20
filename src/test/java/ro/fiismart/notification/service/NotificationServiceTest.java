package ro.fiismart.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.model.Notification;
import ro.fiismart.common.repository.NotificationRepository;
import ro.fiismart.notification.dto.NotificationResponse;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .id("notif1")
                .recipientId("user1")
                .type("ENROLLMENT")
                .title("Test")
                .message("Test message")
                .read(false)
                .courseId("course1")
                .createdAt(new Date())
                .build();
    }

    @Test
    void createEnrollmentNotification_savesNotificationForTeacher() {
        notificationService.createEnrollmentNotification("teacher1", "Ion Popescu", "Matematică", "course1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getRecipientId()).isEqualTo("teacher1");
        assertThat(saved.getType()).isEqualTo("ENROLLMENT");
        assertThat(saved.getCourseId()).isEqualTo("course1");
        assertThat(saved.getMessage()).contains("Ion Popescu");
        assertThat(saved.getMessage()).contains("Matematică");
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void createStudentEnrollmentNotification_savesNotificationForStudent() {
        notificationService.createStudentEnrollmentNotification("student1", "Fizică", "course2");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getRecipientId()).isEqualTo("student1");
        assertThat(saved.getType()).isEqualTo("ENROLLMENT");
        assertThat(saved.getTitle()).isEqualTo("Înrolare confirmată");
        assertThat(saved.getMessage()).contains("Fizică");
    }

    @Test
    void createCommentNotification_savesNotificationWithLectureId() {
        notificationService.createCommentNotification("teacher1", "Ana", "Chimie", "course3", "lecture5");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getRecipientId()).isEqualTo("teacher1");
        assertThat(saved.getType()).isEqualTo("NEW_COMMENT");
        assertThat(saved.getLectureId()).isEqualTo("lecture5");
        assertThat(saved.getMessage()).contains("Ana");
        assertThat(saved.getMessage()).contains("Chimie");
    }

    @Test
    void createCourseUpdateNotification_savesNotification() {
        notificationService.createCourseUpdateNotification("student1", "Biologie", "course4", "Curs actualizat!");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getRecipientId()).isEqualTo("student1");
        assertThat(saved.getType()).isEqualTo("COURSE_UPDATE");
        assertThat(saved.getMessage()).isEqualTo("Curs actualizat!");
    }

    @Test
    void createReplyNotification_savesNotification() {
        notificationService.createReplyNotification("user2", "Maria", "course5");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getRecipientId()).isEqualTo("user2");
        assertThat(saved.getType()).isEqualTo("COMMENT_REPLY");
        assertThat(saved.getMessage()).contains("Maria");
    }

    @Test
    void getForUser_returnsNotificationsAsResponses() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc("user1"))
                .thenReturn(List.of(sampleNotification));

        List<NotificationResponse> result = notificationService.getForUser("user1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("notif1");
        assertThat(result.get(0).getType()).isEqualTo("ENROLLMENT");
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    void getUnreadCount_delegatesToRepository() {
        when(notificationRepository.countByRecipientIdAndReadFalse("user1")).thenReturn(3L);

        long count = notificationService.getUnreadCount("user1");

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void markRead_setsReadTrueAndSaves() {
        when(notificationRepository.findById("notif1")).thenReturn(Optional.of(sampleNotification));

        notificationService.markRead("notif1");

        assertThat(sampleNotification.isRead()).isTrue();
        verify(notificationRepository).save(sampleNotification);
    }

    @Test
    void markRead_doesNothingIfNotFound() {
        when(notificationRepository.findById("notif99")).thenReturn(Optional.empty());

        notificationService.markRead("notif99");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllRead_marksAllUnreadAndSaves() {
        Notification unread1 = Notification.builder().id("n1").recipientId("user1").read(false).build();
        Notification unread2 = Notification.builder().id("n2").recipientId("user1").read(false).build();
        Notification alreadyRead = Notification.builder().id("n3").recipientId("user1").read(true).build();

        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc("user1"))
                .thenReturn(List.of(unread1, unread2, alreadyRead));

        notificationService.markAllRead("user1");

        assertThat(unread1.isRead()).isTrue();
        assertThat(unread2.isRead()).isTrue();
        assertThat(alreadyRead.isRead()).isTrue();
        verify(notificationRepository).saveAll(any());
    }

    @Test
    void persist_doesNotThrowOnRepositoryException() {
        doThrow(new RuntimeException("DB down")).when(notificationRepository).save(any());

        // Should not throw - persist catches and logs
        notificationService.createEnrollmentNotification("teacher1", "Ana", "Curs", "course1");
    }
}
