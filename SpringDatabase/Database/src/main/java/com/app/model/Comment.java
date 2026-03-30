package com.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Comments")
public class Comment {

    @Id
    private String id;
    private String lectureId;
    private String courseId;
    private String authorId;
    private String body;
    private Date createdAt;
    private Date updatedAt;
    @Field("isDeleted")
    private boolean deleted;
    private String parentCommentId;
    private int likeCount;
    @Builder.Default
    private List<String> likedBy = new ArrayList<>();
    @Builder.Default
    private List<ModerationFlag> moderationFlags = new ArrayList<>();
    private int flagCount;
}
