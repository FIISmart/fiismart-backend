package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.*;
import ro.fiismart.common.repository.*;
import ro.fiismart.dashboard.student.dto.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class StudentDashboardService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public StudentDashboardService(QuizAttemptRepository quizAttemptRepository,
                                   CourseRepository courseRepository,
                                   EnrollmentRepository enrollmentRepository,
                                   QuizRepository quizRepository,
                                   CommentRepository commentRepository,
                                   UserRepository userRepository) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public List<QuizStudentDTO> getQuizzesForStudent(String studentId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentId(studentId);
        List<QuizStudentDTO> result = new ArrayList<>();

        for (QuizAttempt attempt : attempts) {
            Quiz quiz = quizRepository.findById(attempt.getQuizId()).orElse(null);
            Course course = courseRepository.findById(attempt.getCourseId()).orElse(null);

            QuizStudentDTO dto = new QuizStudentDTO();
            dto.titluQuiz = quiz != null ? quiz.getTitle() : "Quiz Necunoscut";
            dto.numeCurs = course != null ? course.getTitle() : "Curs Necunoscut";
            dto.incercari = quizAttemptRepository.countByStudentIdAndQuizId(studentId, attempt.getQuizId());
            dto.scor = attempt.getScore();
            dto.status = attempt.isPassed() ? "Promovat" : "Picat";
            result.add(dto);
        }
        return result;
    }

    public ContinueLearningDTO getLastAccessedCourse(String studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        if (enrollments.isEmpty()) return null;

        Enrollment lastAccessed = enrollments.stream()
                .filter(e -> e.getLastAccessedAt() != null)
                .max(Comparator.comparing(Enrollment::getLastAccessedAt))
                .orElse(enrollments.get(0));

        Course course = courseRepository.findById(lastAccessed.getCourseId()).orElse(null);

        ContinueLearningDTO dto = new ContinueLearningDTO();
        if (course != null) {
            dto.setCursId(course.getId());
            dto.setTitluCurs(course.getTitle());
        }
        dto.setProgres(lastAccessed.getOverallProgress());
        return dto;
    }

    public List<StudentAnswerDTO> getAnswersForStudent(String studentId) {
        List<StudentAnswerDTO> result = new ArrayList<>();
        List<Comment> myComments = commentRepository.findByAuthorId(studentId);

        for (Comment question : myComments) {
            List<Comment> replies = commentRepository.findRepliesByParentId(question.getId());
            for (Comment reply : replies) {
                StudentAnswerDTO dto = new StudentAnswerDTO();
                dto.intrebare = question.getBody();
                dto.raspuns = reply.getBody();
                User autor = userRepository.findById(reply.getAuthorId()).orElse(null);
                dto.autorRaspuns = (autor != null) ? autor.getDisplayName() : "Utilizator necunoscut";
                result.add(dto);
            }
        }
        return result;
    }

    public UserNameDTO getStudentName(String studentId) {
        UserNameDTO dto = new UserNameDTO();
        try {
            User user = userRepository.findById(studentId).orElse(null);
            if (user != null && user.getDisplayName() != null) {
                String primulNume = user.getDisplayName().trim().split("\\s+")[0];
                dto.setDisplayName(primulNume);
            } else {
                dto.setDisplayName("User Necunoscut");
            }
        } catch (Exception e) {
            dto.setDisplayName("Eroare la gasirea utilizatorului");
        }
        return dto;
    }
}
