package ro.fiismart.dashboard.student.dto;

import lombok.Data;

@Data
public class QuizStudentDTO {
    public String titluQuiz;
    public String numeCurs;
    public long incercari;
    public int scor;
    public String status;
}
