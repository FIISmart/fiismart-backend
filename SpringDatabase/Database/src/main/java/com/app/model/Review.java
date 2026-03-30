package com.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Review")
public class Review {

    @Id
    private String id;
    private String studentId;
    private String courseId;
    private int stars;
    private String body;
    private Date createdAt;
    @Field("isDeleted")
    private boolean deleted;
    private String deletedBy;
}
