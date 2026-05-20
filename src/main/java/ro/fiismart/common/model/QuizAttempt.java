package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "QuizAttempt")
public class QuizAttempt {

    @Id
    private String id;

    private String quizId;
    private String courseId;
    private String studentId;
    private Date attemptedAt;
    private int score;
    private boolean passed;
    private int timeTakenSecs;

    /**
     * Server-recorded instant when the attempt began (see
     * {@code QuizAttemptService.startAttempt}). Nullable for backwards
     * compatibility — legacy attempts created before the timer/anti-cheat
     * feature do not have a startedAt and are treated as already-final.
     */
    private Instant startedAt;

    /**
     * Lifecycle discriminator for the attempt. Allowed values:
     * <ul>
     *   <li>{@code "IN_PROGRESS"} — student has started but not yet submitted.</li>
     *   <li>{@code "SUBMITTED"}  — student submitted within the time limit.</li>
     *   <li>{@code "EXPIRED"}    — deadline passed (submitted late, or swept).</li>
     *   <li>{@code "ABANDONED"}  — student explicitly walked away.</li>
     * </ul>
     * Modelled as a String (not an enum) to match the project's existing
     * string-discriminator pattern (see {@code ModuleQuiz.quizScope}).
     * Nullable for backwards compat with legacy attempts.
     */
    private String status;

    @Builder.Default
    private List<Answer> answers = new ArrayList<>();
}
