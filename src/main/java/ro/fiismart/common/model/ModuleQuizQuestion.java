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
 * <p>Two question {@code type}s are supported:
 * <ul>
 *   <li>{@code "multiple_choice"} — student picks one of {@link #options};
 *       {@link #correctIdx} holds the index of the correct option.</li>
 *   <li>{@code "written"} — student types a free-form answer;
 *       {@link #correctText} holds the expected answer (compared
 *       case-insensitively, trimmed). For written questions
 *       {@link #correctIdx} is null and {@link #options} is unused.</li>
 * </ul>
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
    /** One of {@code "multiple_choice"} or {@code "written"}. */
    private String type;
    private int points;

    @Builder.Default
    private List<String> options = new ArrayList<>();

    /**
     * Index in {@link #options} of the correct answer for {@code "multiple_choice"} questions.
     * {@code null} for {@code "written"} questions.
     */
    private Integer correctIdx;

    /**
     * Expected answer for {@code "written"} questions (case-insensitive, trimmed match).
     * {@code null} for {@code "multiple_choice"} questions.
     */
    private String correctText;

    /** Optional explanation shown after the student answers. */
    private String explanation;
}
