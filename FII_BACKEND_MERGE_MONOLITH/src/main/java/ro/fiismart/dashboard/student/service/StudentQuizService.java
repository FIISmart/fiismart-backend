package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.common.model.QuizAttempt;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.QuizAttemptRepository;
import ro.fiismart.common.repository.QuizRepository;
import ro.fiismart.dashboard.student.dto.StudentQuizStatusDTO;

import java.util.List;

@Service
public class StudentQuizService {

    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public StudentQuizService(CourseRepository courseRepository,
                              QuizRepository quizRepository,
                              QuizAttemptRepository quizAttemptRepository) {
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public StudentQuizStatusDTO getQuizStatus(String studentId, String courseId) {
        StudentQuizStatusDTO dto = new StudentQuizStatusDTO();

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || course.getQuizId() == null) {
            dto.setHasQuiz(false);
            return dto;
        }

        Quiz quiz = quizRepository.findById(course.getQuizId()).orElse(null);
        if (quiz == null) {
            dto.setHasQuiz(false);
            return dto;
        }

        dto.setHasQuiz(true);
        dto.setQuizId(quiz.getId());

        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentIdAndQuizId(studentId, quiz.getId());
        if (attempts == null || attempts.isEmpty()) {
            dto.setStatus("disponibil");
            return dto;
        }

        QuizAttempt latest = quizAttemptRepository
                .findTopByStudentIdAndQuizIdOrderByAttemptedAtDesc(studentId, quiz.getId())
                .orElse(null);
        dto.setLatestScore(latest != null ? latest.getScore() : null);

        boolean hasPassed = attempts.stream().anyMatch(QuizAttempt::isPassed);
        dto.setStatus(hasPassed ? "promovat" : "picat");
        return dto;
    }
}
