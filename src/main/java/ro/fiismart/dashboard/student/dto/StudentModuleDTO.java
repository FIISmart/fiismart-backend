package ro.fiismart.dashboard.student.dto;

import lombok.Data;
import java.util.List;

@Data
public class StudentModuleDTO {
    private String moduleId;
    private String title;
    private String description;
    private int order;
    private List<StudentLectureDTO> lectures;
    private QuizInfo quiz;

    @Data
    public static class QuizInfo {
        private String quizId;
        private int attemptCount;
        private int lastScore;
        private boolean passed;
        private String statusLabel;
    }
}
