package com.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class QuizQuestionRequest {

    @NotBlank
    private String text;

    @NotBlank
    private String type;

    @Min(1)
    private int points;

    @NotEmpty
    @Builder.Default
    private List<String> options = new ArrayList<>();

    @Min(0)
    private int correctIdx;

    private String explanation;
}
