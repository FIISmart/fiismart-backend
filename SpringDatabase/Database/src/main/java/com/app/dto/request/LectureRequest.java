package com.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class LectureRequest {

    @NotBlank
    private String title;

    private String videoUrl;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Min(0)
    private int order;

    @Min(0)
    private int durationSecs;
}
