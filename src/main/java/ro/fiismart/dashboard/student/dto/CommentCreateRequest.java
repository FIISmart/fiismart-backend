package ro.fiismart.dashboard.student.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentCreateRequest {
    @NotBlank
    private String body;

    @JsonProperty(value = "positionSecs")
    private Integer positionSecs;

    @JsonProperty(value = "timestampSecs")
    public void setTimestampSecs(Integer timestampSecs) {
        if (this.positionSecs == null && timestampSecs != null) {
            this.positionSecs = timestampSecs;
        }
    }

    @JsonProperty(value = "videoTimestamp")
    public void setVideoTimestamp(Integer videoTimestamp) {
        if (this.positionSecs == null && videoTimestamp != null) {
            this.positionSecs = videoTimestamp;
        }
    }
}
