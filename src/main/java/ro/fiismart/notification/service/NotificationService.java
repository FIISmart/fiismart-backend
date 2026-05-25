package ro.fiismart.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.model.Notification;
import ro.fiismart.common.repository.NotificationRepository;
import ro.fiismart.notification.dto.NotificationResponse;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void createEnrollmentNotification(String teacherId, String studentName,
                                              String courseName, String courseId) {
        persist(Notification.builder()
                .recipientId(teacherId)
                .type("ENROLLMENT")
                .title("Student nou înrolat")
                .message(String.format("%s s-a înscris în cursul \"%s\"", studentName, courseName))
                .read(false)
                .courseId(courseId)
                .createdAt(new Date())
                .build());
    }

    public void createStudentEnrollmentNotification(String studentId, String courseName, String courseId) {
        persist(Notification.builder()
                .recipientId(studentId)
                .type("ENROLLMENT")
                .title("Înrolare confirmată")
                .message(String.format("Te-ai înscris cu succes în cursul \"%s\". Mult succes!", courseName))
                .read(false)
                .courseId(courseId)
                .createdAt(new Date())
                .build());
    }

    public void createCommentNotification(String teacherId, String authorName, String courseName,
                                           String courseId, String lectureId) {
        persist(Notification.builder()
                .recipientId(teacherId)
                .type("NEW_COMMENT")
                .title("Comentariu nou")
                .message(String.format("%s a postat un comentariu în cursul \"%s\"", authorName, courseName))
                .read(false)
                .courseId(courseId)
                .lectureId(lectureId)
                .createdAt(new Date())
                .build());
    }

    public void createCourseUpdateNotification(String studentId, String courseName,
                                                String courseId, String message) {
        persist(Notification.builder()
                .recipientId(studentId)
                .type("COURSE_UPDATE")
                .title("Curs actualizat")
                .message(message)
                .read(false)
                .courseId(courseId)
                .createdAt(new Date())
                .build());
    }

    public void createReplyNotification(String recipientId, String replierName, String courseId) {
        persist(Notification.builder()
                .recipientId(recipientId)
                .type("COMMENT_REPLY")
                .title("Răspuns la comentariul tău")
                .message(String.format("%s a răspuns la comentariul tău", replierName))
                .read(false)
                .courseId(courseId)
                .createdAt(new Date())
                .build());
    }

    public List<NotificationResponse> getForUser(String userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    public void markRead(String notificationId, String userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (!n.getRecipientId().equals(userId)) {
                throw new ForbiddenException("You can only mark your own notifications as read");
            }
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllRead(String userId) {
        List<Notification> all = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        all.stream().filter(n -> !n.isRead()).forEach(n -> n.setRead(true));
        notificationRepository.saveAll(all);
    }

    private void persist(Notification n) {
        try {
            notificationRepository.save(n);
        } catch (Exception e) {
            log.warn("Nu s-a putut salva notificarea pentru {}: {}", n.getRecipientId(), e.getMessage());
        }
    }
}
