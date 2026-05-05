package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseModule {

    private String id;
    private String title;
    private String description;
    private int order;

    @Builder.Default
    private List<Lecture> lectures = new ArrayList<>();

    private String quizId;
}
