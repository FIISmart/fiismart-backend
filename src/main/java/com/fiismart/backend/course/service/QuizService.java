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

import java.util.ArrayList;
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

    /**
     * Creaza sau înlocuiește complet quiz-ul unui curs.
     */
    public QuizResponse createOrUpdateQuiz(String courseId, CreateQuizRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        if (!courseDAO.existsById(cid)) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }

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
        if (quiz == null) throw new ResourceNotFoundException("Quiz not found for course: " + courseId);
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

    /**
     * Adaugă o singură întrebare la quiz-ul existent.
     */
    public QuizResponse addQuestion(String courseId, QuizQuestionRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        Quiz quiz = getQuizModelOrThrow(cid, courseId);

        QuizQuestion question = toQuizQuestion(req);
        quizDAO.addQuestion(quiz.getId(), question);

        return QuizResponse.fromModel(quizDAO.findByCourseId(cid));
    }

    /**
     * Șterge o întrebare din quiz după ID-ul ei.
     */
    public void removeQuestion(String courseId, String questionId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId qid = toObjectId(questionId, "Invalid question ID");

        Quiz quiz = getQuizModelOrThrow(cid, courseId);

        boolean exists = quiz.getQuestions().stream().anyMatch(q -> qid.equals(q.getId()));
        if (!exists) throw new ResourceNotFoundException("Question not found: " + questionId);

        quizDAO.removeQuestion(quiz.getId(), qid);
        courseDAO.updateUpdatedAt(cid, new Date());
    }

    /**
     * Reordonează întrebările quiz-ului (pentru drag & drop pe frontend).
     * orderedQuestionIds = lista de ID-uri în noua ordine.
     */
    public QuizResponse reorderQuestions(String courseId, List<String> orderedQuestionIds) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        Quiz quiz = getQuizModelOrThrow(cid, courseId);

        List<QuizQuestion> existing = quiz.getQuestions();
        if (orderedQuestionIds.size() != existing.size()) {
            throw new BadRequestException("Numărul de ID-uri nu corespunde cu numărul de întrebări");
        }

        List<QuizQuestion> reordered = new ArrayList<>();
        for (String qIdStr : orderedQuestionIds) {
            ObjectId qid = toObjectId(qIdStr, "Invalid question ID");
            QuizQuestion found = existing.stream()
                    .filter(q -> qid.equals(q.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + qIdStr));
            reordered.add(found);
        }

        // Înlocuiește lista de întrebări complet
        List<org.bson.Document> questionDocs = reordered.stream()
                .map(QuizQuestion::toDocument)
                .collect(Collectors.toList());

        quizDAO.replaceQuestions(quiz.getId(), questionDocs);
        courseDAO.updateUpdatedAt(cid, new Date());

        return QuizResponse.fromModel(quizDAO.findByCourseId(cid));
    }

    private Quiz getQuizModelOrThrow(ObjectId cid, String courseId) {
        Quiz quiz = quizDAO.findByCourseId(cid);
        if (quiz == null) throw new ResourceNotFoundException("Quiz not found for course: " + courseId);
        return quiz;
    }

    private QuizQuestion toQuizQuestion(QuizQuestionRequest req) {
        return QuizQuestion.builder()
                .id(new ObjectId())
                .text(req.getText())
                .type(req.getType())
                .points(req.getPoints())
                .options(req.getOptions() != null ? req.getOptions() : new java.util.ArrayList<>())
                .correctIdx(req.getCorrectIdx())
                .correctText(req.getCorrectText())
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
