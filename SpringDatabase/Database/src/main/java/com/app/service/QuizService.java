package com.app.service;

import com.app.dto.request.QuizQuestionRequest;
import com.app.dto.request.QuizRequest;
import com.app.dto.response.QuizQuestionResponse;
import com.app.dto.response.QuizResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Quiz;
import com.app.model.QuizQuestion;
import com.app.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
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
        return toResponse(quizRepository.save(quiz));
    }

    public QuizResponse findById(String quizId) {
        return toResponse(quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", quizId)));
    }

    public QuizResponse findByCourseId(String courseId) {
        return toResponse(quizRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found for course: " + courseId)));
    }

    public List<QuizQuestionResponse> findQuestions(String quizId) {
        return quizRepository.findById(quizId)
                .map(q -> q.getQuestions().stream().map(this::toQuestionResponse).toList())
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

    public void updateShuffleQuestions(String quizId, boolean shuffle) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().set("shuffleQuestions", shuffle),
                Quiz.class);
    }

    public QuizQuestionResponse addQuestion(String quizId, QuizQuestionRequest request) {
        QuizQuestion question = QuizQuestion.builder()
                .id(new ObjectId().toHexString())
                .text(request.getText())
                .type(request.getType())
                .points(request.getPoints())
                .options(request.getOptions() != null ? request.getOptions() : new ArrayList<>())
                .correctIdx(request.getCorrectIdx())
                .explanation(request.getExplanation())
                .build();
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().push("questions", question),
                Quiz.class);
        return toQuestionResponse(question);
    }

    public void removeQuestion(String quizId, String questionId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().pull("questions", new org.bson.Document("id", questionId)),
                Quiz.class);
    }

    public void updateQuestionField(String quizId, String questionId, String field, Object value) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId).and("questions.id").is(questionId)),
                new Update().set("questions.$." + field, value),
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

    private QuizResponse toResponse(Quiz quiz) {
        return QuizResponse.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
                .title(quiz.getTitle())
                .passingScore(quiz.getPassingScore())
                .timeLimit(quiz.getTimeLimit())
                .shuffleQuestions(quiz.isShuffleQuestions())
                .questions(quiz.getQuestions() != null
                        ? quiz.getQuestions().stream().map(this::toQuestionResponse).toList()
                        : new ArrayList<>())
                .build();
    }

    private QuizQuestionResponse toQuestionResponse(QuizQuestion q) {
        return QuizQuestionResponse.builder()
                .id(q.getId())
                .text(q.getText())
                .type(q.getType())
                .points(q.getPoints())
                .options(q.getOptions())
                .correctIdx(q.getCorrectIdx())
                .explanation(q.getExplanation())
                .build();
    }
}
