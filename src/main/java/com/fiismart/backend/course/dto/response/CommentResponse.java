package com.fiismart.backend.course.dto.response;

import database.model.Comment;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class CommentResponse {

    private String id;
    private String lectureId;
    private String courseId;
    private String authorId;
    private String body;
    private Date createdAt;
    private Date updatedAt;
    private boolean isDeleted;
    private String parentCommentId;
    private int likeCount;
    private int flagCount;

    public static CommentResponse fromModel(Comment comment) {
        if (comment == null) return null;
        return CommentResponse.builder()
                .id(comment.getId() != null ? comment.getId().toHexString() : null)
                .lectureId(comment.getLectureId() != null ? comment.getLectureId().toHexString() : null)
                .courseId(comment.getCourseId() != null ? comment.getCourseId().toHexString() : null)
                .authorId(comment.getAuthorId() != null ? comment.getAuthorId().toHexString() : null)
                .body(comment.getBody())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isDeleted(comment.isDeleted())
                .parentCommentId(comment.getParentCommentId() != null ? comment.getParentCommentId().toHexString() : null)
                .likeCount(comment.getLikeCount())
                .flagCount(comment.getFlagCount())
                .build();
    }
}
