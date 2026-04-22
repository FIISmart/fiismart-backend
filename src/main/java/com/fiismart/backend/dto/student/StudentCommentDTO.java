package com.fiismart.backend.dto.student;

import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class StudentCommentDTO {
    private String commentId;
    private String authorName;
    private String authorRole;
    private String body;
    private int likeCount;
    private String timeAgo;
    private boolean isLikedByMe;

    @Builder.Default
    private List<StudentCommentDTO> replies = new ArrayList<>();
}