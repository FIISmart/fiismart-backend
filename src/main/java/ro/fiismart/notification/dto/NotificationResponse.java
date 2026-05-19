package ro.fiismart.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.fiismart.common.model.Notification;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private String type;
    private String title;
    private String message;
    private boolean read;
    private String courseId;
    private String lectureId;
    private Date createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .courseId(n.getCourseId())
                .lectureId(n.getLectureId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
