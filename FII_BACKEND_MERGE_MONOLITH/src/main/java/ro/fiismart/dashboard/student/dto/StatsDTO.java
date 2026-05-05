package ro.fiismart.dashboard.student.dto;

import lombok.Data;

@Data
public class StatsDTO {
    private int enrolledCourses;
    private int activeCourses;
    private int quizzesCompleted;
    private int streakDays;
}
