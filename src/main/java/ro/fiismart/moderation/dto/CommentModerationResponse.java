package ro.fiismart.moderation.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.Comment;

import java.util.Date;

@Data
@Builder
public class CommentModerationResponse {

    private String id;
    private String lectureId;
    private String courseId;
    private String authorId;
    private String body;
    private Date createdAt;
    private boolean deleted;
    private String parentCommentId;
    private int likeCount;

    public static CommentModerationResponse fromModel(Comment c) {
        if (c == null) return null;
        return CommentModerationResponse.builder()
                .id(c.getId())
                .lectureId(c.getLectureId())
                .courseId(c.getCourseId())
                .authorId(c.getAuthorId())
                .body(c.getBody())
                .createdAt(c.getCreatedAt())
                .deleted(c.isDeleted())
                .parentCommentId(c.getParentCommentId())
                .likeCount(c.getLikeCount())
                .build();
    }
}
