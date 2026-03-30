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
@Document(collection = "Courses")
public class Course {

    @Id
    private String id;
    private String title;
    private String description;
    private String teacherId;
    private String status;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private String thumbnailUrl;
    private String language;
    private int enrollmentCount;
    private double avgRating;
    @Builder.Default
    private List<Lecture> lectures = new ArrayList<>();
    @Field("isHidden")
    private boolean hidden;
    private String quizId;
    private Date createdAt;
    private Date updatedAt;
}
