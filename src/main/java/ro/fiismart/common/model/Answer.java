package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer {

    private String questionId;
    private int selectedIdx;
    private String writtenAnswer;
    private boolean correct;
}
