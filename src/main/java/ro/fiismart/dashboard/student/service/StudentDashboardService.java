package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ResourceNotFoundException;
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
            Quiz quiz = quizRepository.findById(attempt.getQuizId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz", attempt.getQuizId()));
            Course course = courseRepository.findById(attempt.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course", attempt.getCourseId()));

            QuizStudentDTO dto = new QuizStudentDTO();
            dto.titluQuiz = quiz.getTitle();
            dto.numeCurs = course.getTitle();
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

        Course course = courseRepository.findById(lastAccessed.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", lastAccessed.getCourseId()));

        ContinueLearningDTO dto = new ContinueLearningDTO();
        dto.setCursId(course.getId());
        dto.setTitluCurs(course.getTitle());
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
                User autor = userRepository.findById(reply.getAuthorId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", reply.getAuthorId()));
            dto.autorRaspuns = autor.getDisplayName();
                result.add(dto);
            }
        }
        return result;
    }

    public UserNameDTO getStudentName(String studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));
        UserNameDTO dto = new UserNameDTO();
        if (user.getDisplayName() != null) {
            String primulNume = user.getDisplayName().trim().split("\\s+")[0];
            dto.setDisplayName(primulNume);
        } else {
            dto.setDisplayName("User Necunoscut");
        }
        return dto;
    }
}
