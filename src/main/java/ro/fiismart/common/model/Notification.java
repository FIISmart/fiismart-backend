package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Notifications")
public class Notification {

    @Id
    private String id;

    @Indexed
    private String recipientId;

    private String type;       // ENROLLMENT | NEW_COMMENT | COMMENT_REPLY

    private String title;
    private String message;

    @Builder.Default
    private boolean read = false;

    private String courseId;
    private String lectureId;

    // TTL: MongoDB va șterge automat documentele după 30 de zile
    @Indexed(expireAfterSeconds = 2592000)
    private Date createdAt;
}
