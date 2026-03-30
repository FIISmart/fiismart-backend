package com.app.service;

import com.app.dto.request.QuizAttemptRequest;
import com.app.dto.response.QuizAttemptResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.QuizAttempt;
import com.app.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final MongoTemplate mongoTemplate;

    public QuizAttemptResponse create(QuizAttemptRequest request) {
        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(request.getQuizId())
                .courseId(request.getCourseId())
                .studentId(request.getStudentId())
                .attemptedAt(new Date())
                .score(request.getScore())
                .passed(request.isPassed())
                .timeTakenSecs(request.getTimeTakenSecs())
                .answers(request.getAnswers() != null ? request.getAnswers() : new ArrayList<>())
                .build();
        return toResponse(quizAttemptRepository.save(attempt));
    }

    public QuizAttemptResponse findById(String attemptId) {
        return toResponse(quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("QuizAttempt", attemptId)));
    }

    public List<QuizAttemptResponse> findByStudentId(String studentId) {
        return quizAttemptRepository.findByStudentId(studentId).stream().map(this::toResponse).toList();
    }

    public List<QuizAttemptResponse> findByQuizId(String quizId) {
        return quizAttemptRepository.findByQuizId(quizId).stream().map(this::toResponse).toList();
    }

    public List<QuizAttemptResponse> findByStudentAndQuiz(String studentId, String quizId) {
        return quizAttemptRepository.findByStudentIdAndQuizId(studentId, quizId)
                .stream().map(this::toResponse).toList();
    }

    public List<QuizAttemptResponse> findByStudentAndCourse(String studentId, String courseId) {
        return quizAttemptRepository.findByStudentIdAndCourseId(studentId, courseId)
                .stream().map(this::toResponse).toList();
    }

    public QuizAttemptResponse findLatestAttempt(String studentId, String quizId) {
        QuizAttempt attempt = quizAttemptRepository
                .findFirstByStudentIdAndQuizIdOrderByAttemptedAtDesc(studentId, quizId);
        if (attempt == null) throw new ResourceNotFoundException(
                "No attempt found for student " + studentId + " on quiz " + quizId);
        return toResponse(attempt);
    }

    public List<QuizAttemptResponse> findPassedByQuiz(String quizId) {
        return quizAttemptRepository.findByQuizIdAndPassed(quizId, true)
                .stream().map(this::toResponse).toList();
    }

    public double computeAvgScore(String quizId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizId(quizId);
        if (attempts.isEmpty()) return 0.0;
        double total = attempts.stream().mapToInt(QuizAttempt::getScore).sum();
        return Math.round((total / attempts.size()) * 10.0) / 10.0;
    }

    public void setAnswers(String attemptId, List<com.app.model.Answer> answers) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(attemptId)),
                new Update().set("answers", answers),
                QuizAttempt.class);
    }

    public void deleteById(String attemptId) {
        quizAttemptRepository.deleteById(attemptId);
    }

    public void deleteAllByQuiz(String quizId) {
        quizAttemptRepository.deleteByQuizId(quizId);
    }

    public void deleteAllByCourse(String courseId) {
        quizAttemptRepository.deleteByCourseId(courseId);
    }

    public void deleteAllByStudent(String studentId) {
        quizAttemptRepository.deleteByStudentId(studentId);
    }

    public boolean hasStudentPassedQuiz(String studentId, String quizId) {
        return quizAttemptRepository.existsByStudentIdAndQuizIdAndPassed(studentId, quizId, true);
    }

    public long countByStudentAndQuiz(String studentId, String quizId) {
        return quizAttemptRepository.countByStudentIdAndQuizId(studentId, quizId);
    }

    public long countPassedByQuiz(String quizId) {
        return quizAttemptRepository.countByQuizIdAndPassed(quizId, true);
    }

    private QuizAttemptResponse toResponse(QuizAttempt a) {
        return QuizAttemptResponse.builder()
                .id(a.getId())
                .quizId(a.getQuizId())
                .courseId(a.getCourseId())
                .studentId(a.getStudentId())
                .attemptedAt(a.getAttemptedAt())
                .score(a.getScore())
                .passed(a.isPassed())
                .timeTakenSecs(a.getTimeTakenSecs())
                .answers(a.getAnswers())
                .build();
    }
}
