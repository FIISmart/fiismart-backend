package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Quiz attached to a lecture, a module, or the whole course.
 * Distinguished by the {@link #quizScope} discriminator so a single collection
 * holds all three flavours and we can query the right one with a compound filter.
 *
 * <pre>
 *   quizScope = "lecture"      -> moduleId + lectureId set
 *   quizScope = "module"       -> moduleId set, lectureId null
 *   quizScope = "course_final" -> moduleId + lectureId both null
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ModuleQuiz")
public class ModuleQuiz {

    @Id
    private String id;

    /** Always present. */
    private String courseId;
    /** Present when quizScope is "module" or "lecture". */
    private String moduleId;
    /** Present only when quizScope is "lecture". */
    private String lectureId;

    /** "lecture" | "module" | "course_final" — see class javadoc. */
    private String quizScope;

    private String title;
    private int passingScore;
    private int timeLimit;
    private boolean shuffleQuestions;

    @Builder.Default
    private List<ModuleQuizQuestion> questions = new ArrayList<>();
}
