package com.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lecture {

    private String id;
    private String title;
    private String videoUrl;
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();
    private int order;
    private int durationSecs;
    private Date publishedAt;
}
