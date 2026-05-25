package ro.fiismart.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.common.model.QuizQuestion;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.QuizRepository;
import ro.fiismart.common.util.AuthUtils;
import ro.fiismart.quiz.dto.QuizQuestionRequest;
import ro.fiismart.quiz.dto.QuizQuestionResponse;
import ro.fiismart.quiz.dto.QuizRequest;
import ro.fiismart.quiz.dto.QuizResponse;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizManagementService {

    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;
    private final MongoTemplate mongoTemplate;

    public QuizResponse create(QuizRequest request) {
        verifyCourseOwner(request.getCourseId());
        Quiz quiz = Quiz.builder()
                .courseId(request.getCourseId())
                .title(request.getTitle())
                .passingScore(request.getPassingScore())
                .timeLimit(request.getTimeLimit())
                .shuffleQuestions(request.isShuffleQuestions())
                .questions(new ArrayList<>())
                .build();
        Quiz saved = quizRepository.save(quiz);
        log.info("Quiz created: quizId={} courseId={}", saved.getId(), saved.getCourseId());
        return QuizResponse.fromModel(saved);
    }

    public QuizResponse findById(String quizId) {
        return QuizResponse.fromModel(quizRepository.findById(quizId)
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
        verifyQuizCourseOwner(quizId);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().set("title", title),
                Quiz.class);
    }

    public void updatePassingScore(String quizId, int passingScore) {
        verifyQuizCourseOwner(quizId);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().set("passingScore", passingScore),
                Quiz.class);
    }

    public void updateTimeLimit(String quizId, int timeLimit) {
        verifyQuizCourseOwner(quizId);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().set("timeLimit", timeLimit),
                Quiz.class);
    }

    public QuizQuestionResponse addQuestion(String quizId, QuizQuestionRequest request) {
        verifyQuizCourseOwner(quizId);
        QuizQuestion question = buildQuestion(request);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().push("questions", question),
                Quiz.class);
        return QuizQuestionResponse.fromModel(question);
    }

    public void removeQuestion(String quizId, String questionId) {
        verifyQuizCourseOwner(quizId);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(quizId)),
                new Update().pull("questions", new org.bson.Document("id", questionId)),
                Quiz.class);
    }

    public void deleteById(String quizId) {
        verifyQuizCourseOwner(quizId);
        log.info("Quiz deleted: quizId={}", quizId);
        quizRepository.deleteById(quizId);
    }

    public void deleteByCourseId(String courseId) {
        verifyCourseOwner(courseId);
        log.info("Quizzes deleted for course: courseId={}", courseId);
        quizRepository.deleteByCourseId(courseId);
    }

    public boolean existsByCourseId(String courseId) {
        return quizRepository.existsByCourseId(courseId);
    }

    private void verifyCourseOwner(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        String currentUserId = AuthUtils.getCurrentUserId();
        if (course.getTeacherId() == null || !course.getTeacherId().equals(currentUserId)) {
            throw new ForbiddenException("You are not the owner of this course");
        }
    }

    private void verifyQuizCourseOwner(String quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        verifyCourseOwner(quiz.getCourseId());
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
