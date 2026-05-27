package ro.fiismart.dashboard.student.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Comment;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CommentRepository;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.dashboard.student.dto.CommentCreateRequest;
import ro.fiismart.dashboard.student.dto.StudentCommentDTO;
import ro.fiismart.notification.service.NotificationService;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StudentCommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;
    private final MongoTemplate mongoTemplate;

    public StudentCommentService(CommentRepository commentRepository,
                                 UserRepository userRepository,
                                 CourseRepository courseRepository,
                                 NotificationService notificationService,
                                 MongoTemplate mongoTemplate) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.notificationService = notificationService;
        this.mongoTemplate = mongoTemplate;
    }

    public List<StudentCommentDTO> getCommentsThreaded(String studentId, String lectureId) {
        List<Comment> allComments = commentRepository.findByLectureIdAndNotDeleted(lectureId);

        Set<String> authorIds = allComments.stream().map(Comment::getAuthorId).collect(Collectors.toSet());
        Map<String, User> authorsMap = new HashMap<>();
        for (String id : authorIds) {
            userRepository.findById(id).ifPresent(u -> authorsMap.put(id, u));
        }

        Map<String, StudentCommentDTO> dtoMap = new LinkedHashMap<>();
        for (Comment c : allComments) {
            dtoMap.put(c.getId(), convertToDTO(c, studentId, authorsMap.get(c.getAuthorId())));
        }

        List<StudentCommentDTO> topLevel = new ArrayList<>();
        for (Comment c : allComments) {
            StudentCommentDTO dto = dtoMap.get(c.getId());
            if (c.getParentCommentId() == null) {
                topLevel.add(dto);
            } else {
                StudentCommentDTO parent = dtoMap.get(c.getParentCommentId());
                if (parent != null) parent.getReplies().add(dto);
            }
        }

        topLevel.sort((a, b) -> Integer.compare(b.getLikeCount(), a.getLikeCount()));
        return topLevel;
    }

    public StudentCommentDTO createComment(String studentId, String courseId, String lectureId,
                                           CommentCreateRequest request) {
        Comment comment = Comment.builder()
                .authorId(studentId)
                .courseId(courseId)
                .lectureId(lectureId)
                .body(request.getBody())
                .positionSecs(request.getPositionSecs())
                .createdAt(new Date())
                .deleted(false)
                .likeCount(0)
                .build();

        Comment saved = commentRepository.save(comment);
        User author = userRepository.findById(studentId).orElse(null);
        notifyTeacherOfComment(saved, author);
        return convertToDTO(saved, studentId, author);
    }

    public StudentCommentDTO replyToComment(String studentId, String parentCommentId,
                                            CommentCreateRequest request) {
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("Parent comment not found"));

        Comment reply = Comment.builder()
                .authorId(studentId)
                .courseId(parent.getCourseId())
                .lectureId(parent.getLectureId())
                .parentCommentId(parent.getId())
                .body(request.getBody())
                .positionSecs(request.getPositionSecs())
                .createdAt(new Date())
                .deleted(false)
                .likeCount(0)
                .build();

        Comment saved = commentRepository.save(reply);
        User author = userRepository.findById(studentId).orElse(null);
        notifyReply(parent, studentId, author);
        return convertToDTO(saved, studentId, author);
    }

    /** Notify the course's teacher when a student posts a comment.
     *  Best-effort — never blocks the comment write. */
    private void notifyTeacherOfComment(Comment comment, User author) {
        try {
            if (comment.getCourseId() == null) return;
            Course course = courseRepository.findById(comment.getCourseId()).orElse(null);
            if (course == null || course.getTeacherId() == null) return;
            // Don't notify the teacher about their own comments.
            if (course.getTeacherId().equals(comment.getAuthorId())) return;
            String authorName = author != null && author.getDisplayName() != null
                    ? author.getDisplayName() : "Un student";
            String courseName = course.getTitle() != null ? course.getTitle() : "Curs";
            notificationService.createCommentNotification(
                    course.getTeacherId(), authorName, courseName,
                    comment.getCourseId(), comment.getLectureId());
        } catch (Exception ignored) {
            // Notifications are non-critical.
        }
    }

    /** Notify the parent comment's author when someone replies.
     *  Best-effort — never blocks the reply write. */
    private void notifyReply(Comment parent, String replierId, User replier) {
        try {
            if (parent.getAuthorId() == null) return;
            // Don't notify yourself when replying to your own comment.
            if (parent.getAuthorId().equals(replierId)) return;
            String replierName = replier != null && replier.getDisplayName() != null
                    ? replier.getDisplayName() : "Cineva";
            notificationService.createReplyNotification(
                    parent.getAuthorId(), replierName, parent.getCourseId());
        } catch (Exception ignored) {
            // Notifications are non-critical.
        }
    }

    public void toggleLike(String studentId, String commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return;

        boolean hasLiked = comment.getLikedBy() != null && comment.getLikedBy().contains(studentId);

        if (hasLiked) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(commentId)),
                    new Update().pull("likedBy", studentId).inc("likeCount", -1),
                    Comment.class
            );
        } else {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(commentId)),
                    new Update().addToSet("likedBy", studentId).inc("likeCount", 1),
                    Comment.class
            );
        }
    }

    private StudentCommentDTO convertToDTO(Comment comment, String currentStudentId, User author) {
        String roleLabel = "Elev";
        String authorName = "Utilizator necunoscut";

        if (author != null) {
            authorName = author.getDisplayName() != null ? author.getDisplayName() : "Fără nume";
            if ("teacher".equals(author.getRole())) roleLabel = "Profesor";
            else if ("admin".equals(author.getRole())) roleLabel = "Admin";
        }

        boolean isLiked = comment.getLikedBy() != null && comment.getLikedBy().contains(currentStudentId);

        return StudentCommentDTO.builder()
                .commentId(comment.getId() != null ? comment.getId() : "")
                .authorName(authorName)
                .authorRole(roleLabel)
                .body(comment.getBody() != null ? comment.getBody() : "")
                .likeCount(comment.getLikeCount())
                .timeAgo(getTimeAgo(comment.getCreatedAt()))
                .positionSecs(comment.getPositionSecs())
                .isLikedByMe(isLiked)
                .replies(new ArrayList<>())
                .build();
    }


    private String getTimeAgo(Date past) {
        if (past == null) return "Acum ceva timp";
        long seconds = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime() - past.getTime());
        if (seconds < 60) return "Acum cateva secunde";
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        if (minutes < 60) return "Acum " + minutes + " minute";
        long hours = TimeUnit.MINUTES.toHours(minutes);
        if (hours < 24) return "Acum " + hours + (hours == 1 ? " ora" : " ore");
        long days = TimeUnit.HOURS.toDays(hours);
        return "Acum " + days + (days == 1 ? " zi" : " zile");
    }
}
