package ro.fiismart.tutors.dto;

import lombok.Builder;
import lombok.Data;
import ro.fiismart.common.model.TutorRequest;

import java.util.Date;

@Data
@Builder
public class TutorRequestResponse {
    private String id;
    private String studentId;
    private String studentName;
    private String tutorId;
    private String tutorName;
    private String message;
    private String status;
    private Date createdAt;

    public static TutorRequestResponse fromModel(TutorRequest request) {
        return fromModel(request, null, null);
    }

    public static TutorRequestResponse fromModel(TutorRequest request, String studentName, String tutorName) {
        return TutorRequestResponse.builder()
                .id(request.getId())
                .studentId(request.getStudentId())
                .studentName(studentName)
                .tutorId(request.getTutorId())
                .tutorName(tutorName)
                .message(request.getMessage())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
