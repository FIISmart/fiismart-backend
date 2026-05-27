package ro.fiismart.tutors.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.fiismart.common.model.TutorRequest;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.TutorRequestRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.tutors.dto.TutorRequestCreateRequest;
import ro.fiismart.tutors.dto.TutorRequestResponse;
import ro.fiismart.tutors.dto.TutorRequestStatusUpdateRequest;

import java.util.Date;
import java.util.List;

@Service
public class TutorRequestService {

    private final TutorRequestRepository tutorRequestRepository;
    private final UserRepository userRepository;

    public TutorRequestService(TutorRequestRepository tutorRequestRepository, UserRepository userRepository) {
        this.tutorRequestRepository = tutorRequestRepository;
        this.userRepository = userRepository;
    }

    public TutorRequestResponse create(String studentId, TutorRequestCreateRequest request) {
        User tutor = userRepository.findById(request.getTutorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutorul nu exista."));
        if (!"professor".equalsIgnoreCase(tutor.getRole()) && !"teacher".equalsIgnoreCase(tutor.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilizatorul selectat nu este tutor.");
        }
        if (studentId == null || studentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autentificare necesara.");
        }
        if (tutorRequestRepository.existsByStudentIdAndTutorIdAndStatus(studentId, tutor.getId(), "pending")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exista deja o cerere in asteptare catre acest tutor.");
        }

        TutorRequest saved = tutorRequestRepository.save(TutorRequest.builder()
                .studentId(studentId)
                .tutorId(tutor.getId())
                .message(request.getMessage() == null ? "" : request.getMessage().trim())
                .status("pending")
                .createdAt(new Date())
                .build());
        return toResponse(saved);
    }

    public List<TutorRequestResponse> listForProfessor(String professorId) {
        return tutorRequestRepository.findByTutorId(professorId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TutorRequestResponse> listForStudent(String studentId) {
        return tutorRequestRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    public TutorRequestResponse updateStatus(String professorId, String requestId, TutorRequestStatusUpdateRequest update) {
        TutorRequest request = tutorRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cererea nu exista."));
        if (!professorId.equals(request.getTutorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu poti modifica aceasta cerere.");
        }
        request.setStatus(update.getStatus().toLowerCase());
        return toResponse(tutorRequestRepository.save(request));
    }

    private TutorRequestResponse toResponse(TutorRequest request) {
        String studentName = userRepository.findById(request.getStudentId())
                .map(user -> user.getDisplayName() != null && !user.getDisplayName().isBlank()
                        ? user.getDisplayName()
                        : "Student FII Smart")
                .orElse("Student FII Smart");
        String tutorName = userRepository.findById(request.getTutorId())
                .map(user -> user.getDisplayName() != null && !user.getDisplayName().isBlank()
                        ? user.getDisplayName()
                        : "Profesor FII Smart")
                .orElse("Profesor FII Smart");
        return TutorRequestResponse.fromModel(request, studentName, tutorName);
    }
}
