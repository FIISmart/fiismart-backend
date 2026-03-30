package com.app.service;

import com.app.dto.request.CommentRequest;
import com.app.dto.response.CommentResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Comment;
import com.app.model.ModerationFlag;
import com.app.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final MongoTemplate mongoTemplate;

    public CommentResponse create(CommentRequest request) {
        Comment comment = Comment.builder()
                .lectureId(request.getLectureId())
                .courseId(request.getCourseId())
                .authorId(request.getAuthorId())
                .body(request.getBody())
                .parentCommentId(request.getParentCommentId())
                .createdAt(new Date())
                .updatedAt(new Date())
                .deleted(false)
                .likeCount(0)
                .flagCount(0)
                .likedBy(new ArrayList<>())
                .moderationFlags(new ArrayList<>())
                .build();
        return toResponse(commentRepository.save(comment));
    }

    public CommentResponse findById(String commentId) {
        return toResponse(commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId)));
    }

    public List<CommentResponse> findByLectureId(String lectureId) {
        return commentRepository.findByLectureIdAndDeletedFalse(lectureId)
                .stream().map(this::toResponse).toList();
    }

    public List<CommentResponse> findTopLevelByLectureId(String lectureId) {
        return commentRepository.findByLectureIdAndParentCommentIdIsNullAndDeletedFalse(lectureId)
                .stream().map(this::toResponse).toList();
    }

    public List<CommentResponse> findRepliesByParentId(String parentCommentId) {
        return commentRepository.findByParentCommentIdAndDeletedFalse(parentCommentId)
                .stream().map(this::toResponse).toList();
    }

    public List<CommentResponse> findByAuthorId(String authorId) {
        return commentRepository.findByAuthorIdAndDeletedFalse(authorId)
                .stream().map(this::toResponse).toList();
    }

    public List<CommentResponse> findFlagged(int minFlags) {
        return commentRepository.findByFlagCountGreaterThanEqualAndDeletedFalse(minFlags)
                .stream().map(this::toResponse).toList();
    }

    public void updateBody(String commentId, String newBody) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(commentId)),
                new Update().set("body", newBody).set("updatedAt", new Date()),
                Comment.class);
    }

    public void softDelete(String commentId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(commentId)),
                new Update().set("isDeleted", true),
                Comment.class);
    }

    public void addLike(String commentId, String userId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(commentId).and("likedBy").nin(userId)),
                new Update().addToSet("likedBy", userId).inc("likeCount", 1),
                Comment.class);
    }

    public void removeLike(String commentId, String userId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(commentId).and("likedBy").is(userId)),
                new Update().pull("likedBy", userId).inc("likeCount", -1),
                Comment.class);
    }

    public void addModerationFlag(String commentId, ModerationFlag flag) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(commentId)),
                new Update().push("moderationFlags", flag).inc("flagCount", 1),
                Comment.class);
    }

    public void clearModerationFlags(String commentId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(commentId)),
                new Update().set("moderationFlags", new ArrayList<>()).set("flagCount", 0),
                Comment.class);
    }

    public void deleteById(String commentId) {
        commentRepository.deleteById(commentId);
    }

    public void deleteAllByLecture(String lectureId) {
        commentRepository.deleteByLectureId(lectureId);
    }

    public void deleteAllByCourse(String courseId) {
        commentRepository.deleteByCourseId(courseId);
    }

    public long countByLecture(String lectureId) {
        return commentRepository.countByLectureIdAndDeletedFalse(lectureId);
    }

    public boolean hasUserLiked(String commentId, String userId) {
        return mongoTemplate.exists(
                Query.query(Criteria.where("id").is(commentId).and("likedBy").is(userId)),
                Comment.class);
    }

    private CommentResponse toResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .lectureId(c.getLectureId())
                .courseId(c.getCourseId())
                .authorId(c.getAuthorId())
                .body(c.getBody())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .deleted(c.isDeleted())
                .parentCommentId(c.getParentCommentId())
                .likeCount(c.getLikeCount())
                .likedBy(c.getLikedBy())
                .moderationFlags(c.getModerationFlags())
                .flagCount(c.getFlagCount())
                .build();
    }
}
