package com.fiismart.student.dashboard.service;

import com.fiismart.student.dashboard.dto.ContinueLearningDTO;
import com.fiismart.student.dashboard.dto.QuizStudentDTO;
import com.fiismart.student.dashboard.dto.StudentAnswerDTO;
import database.dao.CourseDAO;
import database.dao.EnrollmentDAO;
import database.dao.QuizAttemptDAO;
import database.dao.QuizDAO;
import database.model.Course;
import database.model.Enrollment;
import database.model.Quiz;
import database.model.QuizAttempt;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {
    private final QuizAttemptDAO quizAttemptDAO = new QuizAttemptDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final QuizDAO quizDAO = new QuizDAO();
    private final database.dao.CommentDAO commentDAO = new database.dao.CommentDAO();
    private final database.dao.UserDAO userDAO = new database.dao.UserDAO();

    public List<QuizStudentDTO> getQuizzesForStudent(String studentIdHex) {
        ObjectId studentId = new ObjectId(studentIdHex);
        List<QuizStudentDTO> result = new ArrayList<>();

        List<QuizAttempt> attempts = quizAttemptDAO.findByStudentId(studentId); //

        for (QuizAttempt attempt : attempts) {
            QuizStudentDTO dto = new QuizStudentDTO();

            Quiz quiz = quizDAO.findById(attempt.getQuizId()); //
            Course curs = courseDAO.findById(attempt.getCourseId()); //

            dto.titluQuiz = quiz != null ? quiz.getTitle() : "Quiz Necunoscut";
            dto.numeCurs = curs != null ? curs.getTitle() : "Curs Necunoscut";
            dto.incercari = quizAttemptDAO.countByStudentAndQuiz(studentId, attempt.getQuizId()); //
            dto.scor = attempt.getScore();
            dto.status = attempt.isPassed() ? "Promovat" : "Picat";

            result.add(dto);
        }

        return result;
    }

    public ContinueLearningDTO getLastAccessedCourse(String studentIdHex) {
        ObjectId studentId = new ObjectId(studentIdHex);

        List<Enrollment> enrollments = enrollmentDAO.findByStudentId(studentId); //

        if (enrollments.isEmpty()) {
            return null;
        }

        Enrollment lastAccessed = enrollments.stream()
                .filter(e -> e.getLastAccessedAt() != null)
                .max(Comparator.comparing(Enrollment::getLastAccessedAt))
                .orElse(enrollments.get(0));

        Course curs = courseDAO.findById(lastAccessed.getCourseId()); //

        ContinueLearningDTO dto = new ContinueLearningDTO();
        if (curs != null) {
            dto.cursId = curs.getId().toHexString();
            dto.titluCurs = curs.getTitle();
        }
        dto.progres = lastAccessed.getOverallProgress();

        return dto;
    }

    public List<StudentAnswerDTO> getAnswersForStudent(String studentIdHex) {
        ObjectId studentId = new ObjectId(studentIdHex);
        List<StudentAnswerDTO> result = new ArrayList<>();

        // 1. Luam toate intrebarile/comentariile pe care le-a pus studentul
        List<database.model.Comment> myComments = commentDAO.findByAuthorId(studentId);

        // 2. Pentru fiecare intrebare a lui, cautam daca a primit un raspuns (un reply)
        for (database.model.Comment question : myComments) {
            List<database.model.Comment> replies = commentDAO.findRepliesByParentId(question.getId());

            // Daca a primit raspunsuri, le adaugam in lista pentru frontend
            for (database.model.Comment reply : replies) {
                StudentAnswerDTO dto = new StudentAnswerDTO();

                dto.intrebare = question.getBody();
                dto.raspuns = reply.getBody();

                // Cautam numele celui care i-a raspuns (profesor sau alt student)
                database.model.User autor = userDAO.findById(reply.getAuthorId());
                dto.autorRaspuns = (autor != null) ? autor.getDisplayName() : "Utilizator necunoscut";

                result.add(dto);
            }
        }

        return result;
    }

}

