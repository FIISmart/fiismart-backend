package ro.fiismart.review.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ReviewResponse {

    private String id;
    private String studentId;
    private String authorName;
    private String courseId;
    private int stars;
    private String body;
    private Date createdAt;
    private boolean deleted;
    private String deletedBy;
}
