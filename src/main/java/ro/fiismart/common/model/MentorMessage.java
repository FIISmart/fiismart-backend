package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorMessage {
    private String id;
    private String senderId;
    private String senderName;
    private String senderRole;
    private String text;
    private Date createdAt;
    private Date readAt;
}
