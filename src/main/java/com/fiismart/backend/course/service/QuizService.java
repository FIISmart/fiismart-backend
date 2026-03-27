package com.fiismart.backend.course.service;

import com.fiismart.backend.course.dto.request.CreateQuizRequest;
import com.fiismart.backend.course.dto.request.QuizQuestionRequest;
import com.fiismart.backend.course.dto.response.QuizResponse;
import com.fiismart.backend.course.exception.BadRequestException;
import com.fiismart.backend.course.exception.ResourceNotFoundException;
import database.dao.CourseDAO;
import database.dao.QuizDAO;
import database.model.Quiz;
import database.model.QuizQuestion;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizDAO quizDAO;
    private final CourseDAO courseDAO;

    public QuizService(QuizDAO quizDAO, CourseDAO courseDAO) {
        this.quizDAO = quizDAO;
        this.courseDAO = courseDAO;
    }

    public QuizResponse createOrUpdateQuiz(String courseId, CreateQuizRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");

        if (!courseDAO.existsById(cid)) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }

        // Delete existing quiz if present
        if (quizDAO.existsByCourseId(cid)) {
            quizDAO.deleteByCourseId(cid);
        }

        List<QuizQuestion> questions = req.getQuestions().stream()
                .map(this::toQuizQuestion)
                .collect(Collectors.toList());

        Quiz quiz = Quiz.builder()
                .id(new ObjectId())
                .courseId(cid)
                .title(req.getTitle())
                .passingScore(req.getPassingScore())
                .timeLimit(req.getTimeLimit())
                .shuffleQuestions(req.isShuffleQuestions())
                .questions(questions)
                .build();

        quizDAO.insert(quiz);
        courseDAO.setQuizId(cid, quiz.getId());
        courseDAO.updateUpdatedAt(cid, new Date());

        return QuizResponse.fromModel(quiz);
    }

    public QuizResponse getQuizByCourseId(String courseId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        Quiz quiz = quizDAO.findByCourseId(cid);
        if (quiz == null) {
            throw new ResourceNotFoundException("Quiz not found for course: " + courseId);
        }
        return QuizResponse.fromModel(quiz);
    }

    public void deleteQuiz(String courseId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");

        if (!quizDAO.existsByCourseId(cid)) {
            throw new ResourceNotFoundException("Quiz not found for course: " + courseId);
        }

        quizDAO.deleteByCourseId(cid);
        courseDAO.setQuizId(cid, null);
        courseDAO.updateUpdatedAt(cid, new Date());
    }

    private QuizQuestion toQuizQuestion(QuizQuestionRequest req) {
        return QuizQuestion.builder()
                .id(new ObjectId())
                .text(req.getText())
                .type(req.getType())
                .points(req.getPoints())
                .options(req.getOptions())
                .correctIdx(req.getCorrectIdx())
                .explanation(req.getExplanation())
                .build();
    }

    private ObjectId toObjectId(String id, String errorMessage) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException(errorMessage + ": " + id);
        }
        return new ObjectId(id);
    }
}
