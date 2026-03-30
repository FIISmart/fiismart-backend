package com.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private String id;
    private String studentId;
    private String courseId;
    private int stars;
    private String body;
    private Date createdAt;
    private boolean deleted;
    private String deletedBy;
}
