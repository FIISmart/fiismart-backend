package com.app.dto.response;

import com.app.model.ModerationFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private String id;
    private String lectureId;
    private String courseId;
    private String authorId;
    private String body;
    private Date createdAt;
    private Date updatedAt;
    private boolean deleted;
    private String parentCommentId;
    private int likeCount;
    private List<String> likedBy;
    private List<ModerationFlag> moderationFlags;
    private int flagCount;
}
