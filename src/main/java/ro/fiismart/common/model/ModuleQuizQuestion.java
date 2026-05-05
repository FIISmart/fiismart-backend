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
    /** Currently only "multiple_choice". */
    private String type;
    private int points;

    @Builder.Default
    private List<String> options = new ArrayList<>();

    /** Index in {@link #options} of the correct answer. */
    private int correctIdx;
    /** Optional explanation shown after the student answers. */
    private String explanation;
}
