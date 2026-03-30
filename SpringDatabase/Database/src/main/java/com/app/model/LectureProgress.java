package com.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureProgress {

    private String lectureId;
    private int watchedSecs;
    private boolean completed;
    private Date lastWatchedAt;
}
