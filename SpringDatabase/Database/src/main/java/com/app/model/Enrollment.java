package com.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Enrollments")
public class Enrollment {

    @Id
    private String id;
    private String studentId;
    private String courseId;
    private Date enrolledAt;
    private Date completedAt;
    private String status;
    @Builder.Default
    private List<LectureProgress> lectureProgress = new ArrayList<>();
    private Date lastAccessedAt;
    private int overallProgress;
}
