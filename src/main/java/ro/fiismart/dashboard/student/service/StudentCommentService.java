package ro.fiismart.dashboard.student.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Comment;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CommentRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.dashboard.student.dto.CommentCreateRequest;
import ro.fiismart.dashboard.student.dto.StudentCommentDTO;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StudentCommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public StudentCommentService(CommentRepository commentRepository,
                                 UserRepository userRepository,
                                 MongoTemplate mongoTemplate) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
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
        log.info("Comment created: commentId={} authorId={} lectureId={}", saved.getId(), saved.getAuthorId(), saved.getLectureId());
        User author = userRepository.findById(studentId).orElse(null);
        return convertToDTO(saved, studentId, author);
    }

    public StudentCommentDTO replyToComment(String studentId, String parentCommentId,
                                            CommentCreateRequest request) {
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", parentCommentId));

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
        log.info("Comment reply created: commentId={} parentCommentId={}", saved.getId(), parentCommentId);
        User author = userRepository.findById(studentId).orElse(null);
        return convertToDTO(saved, studentId, author);
    }

    public void toggleLike(String studentId, String commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return;

        boolean hasLiked = comment.getLikedBy() != null && comment.getLikedBy().contains(studentId);

        log.info("Comment like toggled: commentId={} studentId={}", commentId, studentId);
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
