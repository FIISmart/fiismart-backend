package ro.fiismart.notification.dto;

import org.junit.jupiter.api.Test;
import ro.fiismart.common.model.Notification;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationResponseTest {

    @Test
    void from_mapsAllFields() {
        Date now = new Date();
        Notification n = Notification.builder()
                .id("notif1")
                .type("ENROLLMENT")
                .title("Test Title")
                .message("Test Message")
                .read(true)
                .courseId("course1")
                .lectureId("lecture1")
                .createdAt(now)
                .build();

        NotificationResponse response = NotificationResponse.from(n);

        assertThat(response.getId()).isEqualTo("notif1");
        assertThat(response.getType()).isEqualTo("ENROLLMENT");
        assertThat(response.getTitle()).isEqualTo("Test Title");
        assertThat(response.getMessage()).isEqualTo("Test Message");
        assertThat(response.isRead()).isTrue();
        assertThat(response.getCourseId()).isEqualTo("course1");
        assertThat(response.getLectureId()).isEqualTo("lecture1");
        assertThat(response.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void from_unreadNotification_isReadFalse() {
        Notification n = Notification.builder()
                .id("n2")
                .read(false)
                .build();

        NotificationResponse response = NotificationResponse.from(n);

        assertThat(response.isRead()).isFalse();
    }
}
