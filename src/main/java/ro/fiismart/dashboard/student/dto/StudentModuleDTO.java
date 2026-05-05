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
    private boolean hasQuiz;
    private String quizId;
    private String quizStatus;
    private Integer quizLatestScore;
}
