package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.ModuleQuiz;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.common.model.QuizAttempt;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.ModuleQuizRepository;
import ro.fiismart.common.repository.QuizAttemptRepository;
import ro.fiismart.common.repository.QuizRepository;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.dashboard.student.dto.StudentPlayableQuizDTO;
import ro.fiismart.dashboard.student.dto.StudentQuizStatusDTO;

import java.util.List;

@Service
public class StudentQuizService {

    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final ModuleQuizRepository moduleQuizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public StudentQuizService(CourseRepository courseRepository,
                              QuizRepository quizRepository,
                              ModuleQuizRepository moduleQuizRepository,
                              QuizAttemptRepository quizAttemptRepository) {
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
        this.moduleQuizRepository = moduleQuizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public StudentQuizStatusDTO getQuizStatus(String studentId, String courseId) {
        StudentQuizStatusDTO dto = new StudentQuizStatusDTO();

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return dto;

        ModuleQuiz finalQuiz = moduleQuizRepository.findByCourseIdAndQuizScope(courseId, "course_final").orElse(null);
        if (finalQuiz != null) {
            return buildStatus(finalQuiz.getId(), finalQuiz.getTitle(), finalQuiz.getQuizScope(),
                    finalQuiz.getModuleId(), finalQuiz.getLectureId(), studentId);
        }

        if (course.getQuizId() == null) return dto;
        Quiz quiz = quizRepository.findById(course.getQuizId()).orElse(null);
        if (quiz == null) return dto;

        return buildStatus(quiz.getId(), quiz.getTitle(), "course_final", null, null, studentId);
    }

    public StudentPlayableQuizDTO getPlayableQuiz(String quizId) {
        return moduleQuizRepository.findById(quizId)
                .map(StudentPlayableQuizDTO::fromModuleQuiz)
                .or(() -> quizRepository.findById(quizId).map(StudentPlayableQuizDTO::fromLegacyQuiz))
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
    }

    public StudentQuizStatusDTO buildStatus(String quizId, String title, String scope,
                                            String moduleId, String lectureId, String studentId) {
        StudentQuizStatusDTO dto = new StudentQuizStatusDTO();
        if (quizId == null || quizId.isBlank()) {
            dto.setHasQuiz(false);
            return dto;
        }

        dto.setHasQuiz(true);
        dto.setQuizId(quizId);
        dto.setTitle(title);
        dto.setScope(scope);
        dto.setModuleId(moduleId);
        dto.setLectureId(lectureId);

        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentIdAndQuizId(studentId, quizId);
        dto.setAttemptCount(attempts != null ? attempts.size() : 0);
        if (attempts == null || attempts.isEmpty()) {
            dto.setStatus("disponibil");
            dto.setStatusLabel("Disponibil");
            dto.setPassed(false);
            return dto;
        }

        QuizAttempt latest = quizAttemptRepository
                .findTopByStudentIdAndQuizIdOrderByAttemptedAtDesc(studentId, quizId)
                .orElse(null);
        dto.setLatestScore(latest != null ? latest.getScore() : null);

        boolean hasPassed = attempts.stream().anyMatch(QuizAttempt::isPassed);
        dto.setPassed(hasPassed);
        dto.setStatus(hasPassed ? "promovat" : "picat");
        dto.setStatusLabel((hasPassed ? "Finalizat" : "Incercat") +
                (latest != null ? " - " + latest.getScore() + "%" : ""));
        return dto;
    }
}
