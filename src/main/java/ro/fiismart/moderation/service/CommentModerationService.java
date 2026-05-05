package ro.fiismart.moderation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Comment;
import ro.fiismart.common.repository.CommentRepository;
import ro.fiismart.moderation.dto.CommentModerationResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentModerationService {

    private final CommentRepository commentRepository;
    private final MongoTemplate mongoTemplate;

    public List<CommentModerationResponse> getCommentsByCourseId(String courseId) {
        return commentRepository.findByCourseId(courseId).stream()
                .map(CommentModerationResponse::fromModel)
                .toList();
    }

    public void updateCommentStatus(String commentId, String status) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        String field = switch (status) {
            case "approved" -> "approved";
            case "rejected" -> "rejected";
            case "pending" -> "pending";
            default -> throw new IllegalArgumentException("Invalid status: " + status);
        };

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(commentId)),
                new Update().set("moderationStatus", field),
                Comment.class);
    }

    public void deleteComment(String commentId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(commentId)),
                new Update().set("isDeleted", true),
                Comment.class);
    }
}
