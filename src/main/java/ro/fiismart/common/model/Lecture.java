package ro.fiismart.common.model;

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
    private String moduleId;
    private String title;
    private String type;
    private String content;
    private String videoUrl;
    private String pdfUrl;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    private int order;
    private int durationSecs;
    private Date publishedAt;
}
