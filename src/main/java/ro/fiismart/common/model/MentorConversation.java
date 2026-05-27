package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "MentorConversations")
public class MentorConversation {
    @Id
    private String id;

    @Indexed(unique = true)
    private String requestId;

    @Indexed
    private String studentId;

    @Indexed
    private String tutorId;

    @Builder.Default
    private List<MentorMessage> messages = new ArrayList<>();

    private Date createdAt;
    private Date updatedAt;
}
