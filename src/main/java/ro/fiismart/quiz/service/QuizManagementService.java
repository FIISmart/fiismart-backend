package ro.fiismart.quiz.service;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.common.model.QuizQuestion;
import ro.fiismart.common.repository.ModuleQuizRepository;
import ro.fiismart.common.repository.QuizRepository;
import ro.fiismart.quiz.dto.QuizQuestionRequest;
import ro.fiismart.quiz.dto.QuizQuestionResponse;
import ro.fiismart.quiz.dto.QuizRequest;
import ro.fiismart.quiz.dto.QuizResponse;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizManagementService {

    private final QuizRepository quizRepository;
    private final ModuleQuizRepository moduleQuizRepository;
    private final MongoTemplate mongoTemplate;

    public QuizResponse create(QuizRequest request) {
        Quiz quiz = Quiz.builder()
                .courseId(request.getCourseId())
                .title(request.getTitle())
                .passingScore(request.getPassingScore())
                .timeLimit(request.getTimeLimit())
                .shuffleQuestions(request.isShuffleQuestions())
                .questions(new ArrayList<>())
                .build();
        return QuizResponse.fromModel(quizRepository.save(quiz));
    }

    public QuizResponse findById(String quizId) {
        return quizRepository.findById(quizId)
                .map(QuizResponse::fromModel)
                .orElseGet(() -> moduleQuizRepository.findById(quizId)
                        .map(QuizResponse::fromModuleQuiz)
                        .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId)));
    }

    public QuizResponse findByCourseId(String courseId) {
        return QuizResponse.fromModel(quizRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found for course: " + courseId)));
    }

    public List<QuizQuestionResponse> findQuestions(String quizId) {
        return quizRepository.findById(quizId)
                .map(q -> q.getQuestions().stream().map(QuizQuestionResponse::fromModel).toList())
                .orElse(new ArrayList<>());
    }

    public void updateTitle(String quizId, String title) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().set("title", title),
                Quiz.class);
    }

    public void updatePassingScore(String quizId, int passingScore) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().set("passingScore", passingScore),
                Quiz.class);
    }

    public void updateTimeLimit(String quizId, int timeLimit) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().set("timeLimit", timeLimit),
                Quiz.class);
    }

    public QuizQuestionResponse addQuestion(String quizId, QuizQuestionRequest request) {
        QuizQuestion question = buildQuestion(request);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().push("questions", question),
                Quiz.class);
        return QuizQuestionResponse.fromModel(question);
    }

    public void removeQuestion(String quizId, String questionId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().pull("questions", new org.bson.Document("id", questionId)),
                Quiz.class);
    }

    public void deleteById(String quizId) {
        quizRepository.deleteById(quizId);
    }

    public void deleteByCourseId(String courseId) {
        quizRepository.deleteByCourseId(courseId);
    }

    public boolean existsByCourseId(String courseId) {
        return quizRepository.existsByCourseId(courseId);
    }

    private QuizQuestion buildQuestion(QuizQuestionRequest req) {
        return QuizQuestion.builder()
                .id(new ObjectId().toHexString())
                .text(req.getText())
                .type(req.getType())
                .points(req.getPoints())
                .options(req.getOptions() != null ? req.getOptions() : new ArrayList<>())
                .correctIdx(req.getCorrectIdx())
                .correctText(req.getCorrectText())
                .explanation(req.getExplanation())
                .build();
    }
}
