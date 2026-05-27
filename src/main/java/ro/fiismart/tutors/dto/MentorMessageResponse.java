package ro.fiismart.tutors.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.MentorMessage;

import java.util.Date;

@Data
@Builder
public class MentorMessageResponse {
    private String id;
    private String senderId;
    private String senderName;
    private String senderRole;
    private String text;
    private Date createdAt;

    public static MentorMessageResponse fromModel(MentorMessage message) {
        return MentorMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .senderRole(message.getSenderRole())
                .text(message.getText())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
