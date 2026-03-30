package com.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank
    private String lectureId;

    @NotBlank
    private String courseId;

    @NotBlank
    private String authorId;

    @NotBlank
    private String body;

    private String parentCommentId;
}
