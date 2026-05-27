package ro.fiismart.tutors.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.fiismart.common.model.MentorConversation;
import ro.fiismart.common.model.MentorMessage;
import ro.fiismart.common.model.TutorRequest;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.MentorConversationRepository;
import ro.fiismart.common.repository.TutorRequestRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.tutors.dto.MentorConversationResponse;
import ro.fiismart.tutors.dto.MentorMessageRequest;
import ro.fiismart.tutors.dto.MentorMessageResponse;
import ro.fiismart.tutors.dto.TutorRequestCreateRequest;
import ro.fiismart.tutors.dto.TutorRequestResponse;
import ro.fiismart.tutors.dto.TutorRequestStatusUpdateRequest;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class TutorRequestService {

    private final TutorRequestRepository tutorRequestRepository;
    private final MentorConversationRepository mentorConversationRepository;
    private final UserRepository userRepository;

    public TutorRequestService(TutorRequestRepository tutorRequestRepository,
                               MentorConversationRepository mentorConversationRepository,
                               UserRepository userRepository) {
        this.tutorRequestRepository = tutorRequestRepository;
        this.mentorConversationRepository = mentorConversationRepository;
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
        String status = normalizeStatus(update.getStatus());
        request.setStatus(status);
        if ("accepted".equals(status)) {
            MentorConversation conversation = ensureConversation(request);
            request.setConversationId(conversation.getId());
        }
        return toResponse(tutorRequestRepository.save(request));
    }

    public MentorConversationResponse getConversation(String userId, String requestId) {
        TutorRequest request = loadAccessibleRequest(userId, requestId);
        if (!allowsConversation(request)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversatia este disponibila doar pentru cereri acceptate.");
        }
        MentorConversation conversation = ensureConversation(request);
        if (request.getConversationId() == null || !request.getConversationId().equals(conversation.getId())) {
            request.setConversationId(conversation.getId());
            tutorRequestRepository.save(request);
        }
        return toConversationResponse(conversation);
    }

    public List<MentorMessageResponse> listMessages(String userId, String conversationId) {
        MentorConversation conversation = loadAccessibleConversation(userId, conversationId);
        return (conversation.getMessages() == null ? List.<MentorMessage>of() : conversation.getMessages())
                .stream()
                .map(MentorMessageResponse::fromModel)
                .toList();
    }

    public MentorMessageResponse sendMessage(String userId, String conversationId, MentorMessageRequest body) {
        MentorConversation conversation = loadAccessibleConversation(userId, conversationId);
        TutorRequest request = tutorRequestRepository.findById(conversation.getRequestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cererea nu exista."));
        if (!allowsConversation(request)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversatia este disponibila doar pentru cereri acceptate.");
        }
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizatorul nu exista."));
        MentorMessage message = MentorMessage.builder()
                .id(UUID.randomUUID().toString())
                .senderId(userId)
                .senderName(displayName(sender, "Utilizator FII Smart"))
                .senderRole(sender.getRole())
                .text(body.getText().trim())
                .createdAt(new Date())
                .build();
        if (conversation.getMessages() == null) {
            conversation.setMessages(new java.util.ArrayList<>());
        }
        conversation.getMessages().add(message);
        conversation.setUpdatedAt(new Date());
        mentorConversationRepository.save(conversation);
        return MentorMessageResponse.fromModel(message);
    }

    private TutorRequestResponse toResponse(TutorRequest request) {
        String studentName = userRepository.findById(request.getStudentId())
                .map(user -> displayName(user, "Student FII Smart"))
                .orElse("Student FII Smart");
        String tutorName = userRepository.findById(request.getTutorId())
                .map(user -> displayName(user, "Profesor FII Smart"))
                .orElse("Profesor FII Smart");
        return TutorRequestResponse.fromModel(request, studentName, tutorName);
    }

    private TutorRequest loadAccessibleRequest(String userId, String requestId) {
        TutorRequest request = tutorRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cererea nu exista."));
        if (!userId.equals(request.getStudentId()) && !userId.equals(request.getTutorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu poti accesa aceasta cerere.");
        }
        return request;
    }

    private MentorConversation loadAccessibleConversation(String userId, String conversationId) {
        return mentorConversationRepository
                .findByIdAndStudentIdOrIdAndTutorId(conversationId, userId, conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversatia nu exista."));
    }

    private MentorConversation ensureConversation(TutorRequest request) {
        if (request.getConversationId() != null && !request.getConversationId().isBlank()) {
            return mentorConversationRepository.findById(request.getConversationId())
                    .orElseGet(() -> createConversation(request));
        }
        return mentorConversationRepository.findByRequestId(request.getId())
                .orElseGet(() -> createConversation(request));
    }

    private MentorConversation createConversation(TutorRequest request) {
        Date now = new Date();
        return mentorConversationRepository.save(MentorConversation.builder()
                .requestId(request.getId())
                .studentId(request.getStudentId())
                .tutorId(request.getTutorId())
                .messages(new java.util.ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private MentorConversationResponse toConversationResponse(MentorConversation conversation) {
        String studentName = userRepository.findById(conversation.getStudentId())
                .map(user -> displayName(user, "Student FII Smart"))
                .orElse("Student FII Smart");
        String tutorName = userRepository.findById(conversation.getTutorId())
                .map(user -> displayName(user, "Profesor FII Smart"))
                .orElse("Profesor FII Smart");
        return MentorConversationResponse.fromModel(conversation, studentName, tutorName);
    }

    private boolean allowsConversation(TutorRequest request) {
        String status = normalizeStatus(request.getStatus());
        return "accepted".equals(status) || "resolved".equals(status);
    }

    private String normalizeStatus(String status) {
        if (status == null) return "pending";
        return status.trim().toLowerCase();
    }

    private String displayName(User user, String fallback) {
        return user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : fallback;
    }
}
