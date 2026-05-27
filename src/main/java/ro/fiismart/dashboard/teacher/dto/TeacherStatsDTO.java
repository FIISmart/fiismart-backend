package ro.fiismart.dashboard.teacher.dto;

import lombok.Data;

@Data
public class TeacherStatsDTO {
    private int studentsEnrolled;
    private int activeCourses;
    private int quizzesCompleted;
    private int totalQuizzes;
    private int lectureQuizzes;
    private int moduleQuizzes;
    private int finalQuizzes;
    private double completionRatePct;
}
