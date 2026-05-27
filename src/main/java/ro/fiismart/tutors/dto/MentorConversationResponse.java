package ro.fiismart.tutors.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.MentorConversation;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class MentorConversationResponse {
    private String id;
    private String requestId;
    private String studentId;
    private String studentName;
    private String tutorId;
    private String tutorName;
    private List<MentorMessageResponse> messages;
    private Date createdAt;
    private Date updatedAt;

    public static MentorConversationResponse fromModel(
            MentorConversation conversation,
            String studentName,
            String tutorName
    ) {
        return MentorConversationResponse.builder()
                .id(conversation.getId())
                .requestId(conversation.getRequestId())
                .studentId(conversation.getStudentId())
                .studentName(studentName)
                .tutorId(conversation.getTutorId())
                .tutorName(tutorName)
                .messages((conversation.getMessages() == null ? List.<ro.fiismart.common.model.MentorMessage>of() : conversation.getMessages())
                        .stream()
                        .map(MentorMessageResponse::fromModel)
                        .toList())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
