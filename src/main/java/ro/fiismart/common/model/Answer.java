package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One row inside {@link QuizAttempt#getAnswers()}. Captures the student's
 * submission for a single question plus the grading verdict.
 *
 * <p>The {@code ai*} fields are populated only when the question is of
 * {@code type == "free_text"} — they hold the rubric-based AI evaluation.
 * All AI fields are nullable so legacy answers (and any failed grading
 * runs) deserialize cleanly. {@link #correct} stays the canonical boolean
 * verdict: for free-text answers it is derived from {@link #aiScore} vs
 * the question's {@code passThreshold} and {@link #aiConfidence} ≥ 0.7.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer {

    private String questionId;
    private int selectedIdx;
    private String writtenAnswer;
    private boolean correct;

    /** AI-graded score 0-100 for {@code free_text} questions; null if grading failed. */
    private Double aiScore;

    /** AI confidence in the score, 0-1; null if grading failed. */
    private Double aiConfidence;

    /** Short Romanian feedback shown to the student; nullable. */
    private String aiReasoning;

    /** Key concepts the AI flagged as missing from the student's answer; nullable. */
    private List<String> aiMissingConcepts;
}
