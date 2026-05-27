package ro.fiismart.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "TutorRequests")
public class TutorRequest {
    @Id
    private String id;
    private String studentId;
    private String tutorId;
    private String message;
    private String status;
    private Date createdAt;
}
