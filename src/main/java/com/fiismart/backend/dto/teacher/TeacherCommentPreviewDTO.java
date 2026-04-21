package com.fiismart.backend.dto.teacher;

import lombok.Data;

import java.util.Date;

@Data
public class TeacherCommentPreviewDTO {
    private String commentId;
    private String courseId;
    private String courseTitle;
    private String lectureId;
    private String authorId;
    private String authorDisplayName;
    private String body;
    private Date createdAt;
    private int likeCount;
    private int repliesCount;
    private boolean isAnswered;
}
