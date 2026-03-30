package com.app.dto.response;

import com.app.model.LectureProgress;
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
public class EnrollmentResponse {

    private String id;
    private String studentId;
    private String courseId;
    private Date enrolledAt;
    private Date completedAt;
    private String status;
    private List<LectureProgress> lectureProgress;
    private Date lastAccessedAt;
    private int overallProgress;
}
