package com.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureResponse {

    private String id;
    private String title;
    private String videoUrl;
    private List<String> imageUrls;
    private int order;
    private int durationSecs;
    private Date publishedAt;
}
