package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedded inside {@link ModuleQuiz}. One row per quiz question.
 * Supports an optional imageUrl on top of the standard text + multiple-choice fields.
 *
 * <p>Three question {@code type}s are supported:
 * <ul>
 *   <li>{@code "multiple_choice"} — student picks one of {@link #options};
 *       {@link #correctIdx} holds the index of the correct option.</li>
 *   <li>{@code "written"} — legacy exact-match: student types a free-form answer;
 *       {@link #correctText} holds the expected answer (compared
 *       case-insensitively, trimmed). For written questions
 *       {@link #correctIdx} is null and {@link #options} is unused.</li>
 *   <li>{@code "free_text"} — AI-graded rubric: professor supplies
 *       {@link #sampleAnswer}, {@link #keyConcepts} and {@link #passThreshold};
 *       Gemini scores the student answer against the rubric.</li>
 * </ul>
 *
 * <p>All {@code free_text}-only fields ({@link #sampleAnswer},
 * {@link #keyConcepts}, {@link #passThreshold}) are nullable so legacy
 * documents that pre-date this feature continue to deserialize cleanly.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleQuizQuestion {

    private String id;
    private String text;
    /** Optional image URL displayed alongside the question. */
    private String imageUrl;
    /** One of {@code "multiple_choice"}, {@code "written"} (legacy exact-match)
     *  or {@code "free_text"} (AI-graded). */
    private String type;
    private int points;

    @Builder.Default
    private List<String> options = new ArrayList<>();

    /**
     * Index in {@link #options} of the correct answer for {@code "multiple_choice"} questions.
     * {@code null} for {@code "written"} and {@code "free_text"} questions.
     */
    private Integer correctIdx;

    /**
     * Expected answer for {@code "written"} questions (case-insensitive, trimmed match).
     * {@code null} for {@code "multiple_choice"} and {@code "free_text"} questions.
     */
    private String correctText;

    /**
     * Reference answer used by the AI grader when {@code type == "free_text"}.
     * Nullable — only populated for free-text questions.
     */
    private String sampleAnswer;

    /**
     * Required concepts the student answer must cover for {@code type == "free_text"}.
     * Nullable — only populated for free-text questions.
     */
    private List<String> keyConcepts;

    /**
     * Pass threshold (0-100) for {@code type == "free_text"}: the AI must
     * score the answer at least this high (with confidence ≥ 0.7) for the
     * question to count as correct. Nullable — only populated for free-text
     * questions; defaults to 70 if absent.
     */
    private Integer passThreshold;

    /** Optional explanation shown after the student answers. */
    private String explanation;
}
