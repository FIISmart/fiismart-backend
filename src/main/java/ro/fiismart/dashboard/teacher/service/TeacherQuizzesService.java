package ro.fiismart.dashboard.teacher.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.common.model.QuizAttempt;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.QuizAttemptRepository;
import ro.fiismart.common.repository.QuizRepository;
import ro.fiismart.dashboard.teacher.dto.TeacherQuizPreviewDTO;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeacherQuizzesService {

    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public TeacherQuizzesService(CourseRepository courseRepository,
                                 QuizRepository quizRepository,
                                 QuizAttemptRepository quizAttemptRepository) {
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public List<TeacherQuizPreviewDTO> getQuizzes(String teacherId, int limit, int offset) {
        List<Course> courses = courseRepository.findByTeacherId(teacherId);

        List<TeacherQuizPreviewDTO> result = new ArrayList<>();
        for (Course course : courses) {
            Quiz quiz = quizRepository.findByCourseId(course.getId()).orElse(null);
            if (quiz == null) continue;

            List<QuizAttempt> attempts = quizAttemptRepository.findByQuizId(quiz.getId());
            int attemptCount = attempts.size();
            double avgScore = attempts.isEmpty()
                    ? 0
                    : attempts.stream().mapToDouble(QuizAttempt::getScore).average().orElse(0);

            TeacherQuizPreviewDTO dto = new TeacherQuizPreviewDTO();
            dto.setQuizId(quiz.getId());
            dto.setTitle(quiz.getTitle());
            dto.setCourseId(course.getId());
            dto.setCourseTitle(course.getTitle());
            dto.setAttemptsCount(attemptCount);
            dto.setAvgScorePct(avgScore);
            dto.setStatus("active");
            result.add(dto);
        }

        return result.stream().skip(offset).limit(limit).toList();
    }
}
