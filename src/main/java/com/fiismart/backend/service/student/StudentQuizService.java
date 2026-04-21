package com.fiismart.backend.service.student;

import com.fiismart.backend.dto.student.StudentQuizStatusDTO;
import database.dao.CourseDAO;
import database.dao.QuizAttemptDAO;
import database.dao.QuizDAO;
import database.model.Course;
import database.model.Quiz;
import database.model.QuizAttempt;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentQuizService {

    private final CourseDAO courseDAO = new CourseDAO();
    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizAttemptDAO quizAttemptDAO = new QuizAttemptDAO();

    public StudentQuizStatusDTO getQuizStatus(String studentIdHex, String courseIdHex) {
        ObjectId studentId = new ObjectId(studentIdHex);
        ObjectId courseId = new ObjectId(courseIdHex);

        StudentQuizStatusDTO dto = new StudentQuizStatusDTO();

        Course course = courseDAO.findById(courseId);
        if (course == null || course.getQuizId() == null) {
            dto.setHasQuiz(false);
            dto.setQuizId(null);
            dto.setStatus(null);
            dto.setLatestScore(null);
            return dto;
        }

        Quiz quiz = quizDAO.findById(course.getQuizId());
        if (quiz == null) {
            dto.setHasQuiz(false);
            dto.setQuizId(null);
            dto.setStatus(null);
            dto.setLatestScore(null);
            return dto;
        }

        dto.setHasQuiz(true);
        dto.setQuizId(quiz.getId().toHexString());

        List<QuizAttempt> attempts = quizAttemptDAO.findByStudentAndQuiz(studentId, quiz.getId());

        if (attempts == null || attempts.isEmpty()) {
            dto.setStatus("disponibil");
            dto.setLatestScore(null);
            return dto;
        }

        QuizAttempt latest = quizAttemptDAO.findLatestAttempt(studentId, quiz.getId());
        dto.setLatestScore(latest != null ? latest.getScore() : null);

        boolean hasPassed = attempts.stream().anyMatch(QuizAttempt::isPassed);
        dto.setStatus(hasPassed ? "promovat" : "picat");

        return dto;
    }
}