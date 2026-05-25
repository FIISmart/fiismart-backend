package ro.fiismart.dashboard.student.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StreakResponse {
    private int currentStreak;
    private boolean hasCompletedToday;
}
