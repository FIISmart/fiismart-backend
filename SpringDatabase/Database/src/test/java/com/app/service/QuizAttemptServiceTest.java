package com.app.service;

import com.app.dto.request.QuizAttemptRequest;
import com.app.dto.response.QuizAttemptResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Answer;
import com.app.model.QuizAttempt;
import com.app.repository.QuizAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private QuizAttemptService quizAttemptService;

    private QuizAttempt buildAttempt(String id, String quizId, String studentId, int score, boolean passed) {
        return QuizAttempt.builder()
                .id(id)
                .quizId(quizId)
                .courseId("c1")
                .studentId(studentId)
                .attemptedAt(new Date())
                .score(score)
                .passed(passed)
                .timeTakenSecs(120)
                .answers(new ArrayList<>())
                .build();
    }

    @Test
    void create_savesAttemptAndReturnsResponse() {
        Answer answer = Answer.builder().questionId("q1").selectedIdx(0).correct(true).build();
        QuizAttemptRequest request = QuizAttemptRequest.builder()
                .quizId("quiz1")
                .courseId("c1")
                .studentId("s1")
                .score(85)
                .passed(true)
                .timeTakenSecs(300)
                .answers(List.of(answer))
                .build();

        QuizAttempt saved = buildAttempt("a1", "quiz1", "s1", 85, true);
        saved.setAnswers(List.of(answer));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(saved);

        QuizAttemptResponse response = quizAttemptService.create(request);

        assertThat(response.getId()).isEqualTo("a1");
        assertThat(response.getQuizId()).isEqualTo("quiz1");
        assertThat(response.getStudentId()).isEqualTo("s1");
        assertThat(response.getScore()).isEqualTo(85);
        assertThat(response.isPassed()).isTrue();
        verify(quizAttemptRepository).save(any(QuizAttempt.class));
    }

    @Test
    void create_withNullAnswers_initializesEmptyList() {
        QuizAttemptRequest request = QuizAttemptRequest.builder()
                .quizId("quiz1")
                .courseId("c1")
                .studentId("s1")
                .score(50)
                .passed(false)
                .timeTakenSecs(200)
                .answers(null)
                .build();

        QuizAttempt saved = buildAttempt("a1", "quiz1", "s1", 50, false);
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(saved);

        ArgumentCaptor<QuizAttempt> captor = ArgumentCaptor.forClass(QuizAttempt.class);
        quizAttemptService.create(request);
        verify(quizAttemptRepository).save(captor.capture());

        assertThat(captor.getValue().getAnswers()).isNotNull().isEmpty();
    }

    @Test
    void create_setsAttemptedAt() {
        QuizAttemptRequest request = QuizAttemptRequest.builder()
                .quizId("quiz1")
                .courseId("c1")
                .studentId("s1")
                .score(60)
                .passed(false)
                .timeTakenSecs(150)
                .build();

        QuizAttempt saved = buildAttempt("a1", "quiz1", "s1", 60, false);
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(saved);

        ArgumentCaptor<QuizAttempt> captor = ArgumentCaptor.forClass(QuizAttempt.class);
        quizAttemptService.create(request);
        verify(quizAttemptRepository).save(captor.capture());

        assertThat(captor.getValue().getAttemptedAt()).isNotNull();
    }

    @Test
    void findById_returnsResponse_whenFound() {
        QuizAttempt attempt = buildAttempt("a1", "q1", "s1", 90, true);
        when(quizAttemptRepository.findById("a1")).thenReturn(Optional.of(attempt));

        QuizAttemptResponse response = quizAttemptService.findById("a1");

        assertThat(response.getId()).isEqualTo("a1");
        assertThat(response.getScore()).isEqualTo(90);
    }

    @Test
    void findById_throwsResourceNotFoundException_whenNotFound() {
        when(quizAttemptRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizAttemptService.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("QuizAttempt")
                .hasMessageContaining("missing");
    }

    @Test
    void findByStudentId_returnsList() {
        List<QuizAttempt> attempts = List.of(
                buildAttempt("a1", "q1", "s1", 70, true),
                buildAttempt("a2", "q2", "s1", 50, false)
        );
        when(quizAttemptRepository.findByStudentId("s1")).thenReturn(attempts);

        List<QuizAttemptResponse> responses = quizAttemptService.findByStudentId("s1");

        assertThat(responses).hasSize(2);
    }

    @Test
    void findByStudentId_returnsEmptyList_whenNone() {
        when(quizAttemptRepository.findByStudentId("s99")).thenReturn(List.of());

        assertThat(quizAttemptService.findByStudentId("s99")).isEmpty();
    }

    @Test
    void findByQuizId_returnsList() {
        List<QuizAttempt> attempts = List.of(
                buildAttempt("a1", "q1", "s1", 80, true),
                buildAttempt("a2", "q1", "s2", 60, false)
        );
        when(quizAttemptRepository.findByQuizId("q1")).thenReturn(attempts);

        List<QuizAttemptResponse> responses = quizAttemptService.findByQuizId("q1");

        assertThat(responses).hasSize(2);
    }

    @Test
    void findByStudentAndQuiz_returnsList() {
        List<QuizAttempt> attempts = List.of(
                buildAttempt("a1", "q1", "s1", 75, true)
        );
        when(quizAttemptRepository.findByStudentIdAndQuizId("s1", "q1")).thenReturn(attempts);

        List<QuizAttemptResponse> responses = quizAttemptService.findByStudentAndQuiz("s1", "q1");

        assertThat(responses).hasSize(1);
    }

    @Test
    void findByStudentAndCourse_returnsList() {
        List<QuizAttempt> attempts = List.of(buildAttempt("a1", "q1", "s1", 80, true));
        when(quizAttemptRepository.findByStudentIdAndCourseId("s1", "c1")).thenReturn(attempts);

        List<QuizAttemptResponse> responses = quizAttemptService.findByStudentAndCourse("s1", "c1");

        assertThat(responses).hasSize(1);
    }

    @Test
    void findLatestAttempt_returnsResponse_whenAttemptExists() {
        QuizAttempt attempt = buildAttempt("a1", "q1", "s1", 85, true);
        when(quizAttemptRepository.findFirstByStudentIdAndQuizIdOrderByAttemptedAtDesc("s1", "q1"))
                .thenReturn(attempt);

        QuizAttemptResponse response = quizAttemptService.findLatestAttempt("s1", "q1");

        assertThat(response.getId()).isEqualTo("a1");
    }

    @Test
    void findLatestAttempt_throwsResourceNotFoundException_whenNoAttempt() {
        when(quizAttemptRepository.findFirstByStudentIdAndQuizIdOrderByAttemptedAtDesc("s1", "q1"))
                .thenReturn(null);

        assertThatThrownBy(() -> quizAttemptService.findLatestAttempt("s1", "q1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("s1")
                .hasMessageContaining("q1");
    }

    @Test
    void findPassedByQuiz_returnsPassedAttempts() {
        List<QuizAttempt> passed = List.of(buildAttempt("a1", "q1", "s1", 90, true));
        when(quizAttemptRepository.findByQuizIdAndPassed("q1", true)).thenReturn(passed);

        List<QuizAttemptResponse> responses = quizAttemptService.findPassedByQuiz("q1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isPassed()).isTrue();
    }

    @Test
    void findPassedByQuiz_returnsEmptyList_whenNonePassed() {
        when(quizAttemptRepository.findByQuizIdAndPassed("q1", true)).thenReturn(List.of());

        assertThat(quizAttemptService.findPassedByQuiz("q1")).isEmpty();
    }

    @Test
    void computeAvgScore_returnsZero_whenNoAttempts() {
        when(quizAttemptRepository.findByQuizId("q1")).thenReturn(List.of());

        double avg = quizAttemptService.computeAvgScore("q1");

        assertThat(avg).isEqualTo(0.0);
    }

    @Test
    void computeAvgScore_returnsSingleScore_whenOneAttempt() {
        when(quizAttemptRepository.findByQuizId("q1"))
                .thenReturn(List.of(buildAttempt("a1", "q1", "s1", 80, true)));

        double avg = quizAttemptService.computeAvgScore("q1");

        assertThat(avg).isEqualTo(80.0);
    }

    @Test
    void computeAvgScore_returnsAverage_whenMultipleAttempts() {
        List<QuizAttempt> attempts = List.of(
                buildAttempt("a1", "q1", "s1", 70, false),
                buildAttempt("a2", "q1", "s2", 90, true)
        );
        when(quizAttemptRepository.findByQuizId("q1")).thenReturn(attempts);

        double avg = quizAttemptService.computeAvgScore("q1");

        assertThat(avg).isEqualTo(80.0);
    }

    @Test
    void computeAvgScore_roundsToOneDecimalPlace() {
        List<QuizAttempt> attempts = List.of(
                buildAttempt("a1", "q1", "s1", 10, false),
                buildAttempt("a2", "q1", "s2", 20, false),
                buildAttempt("a3", "q1", "s3", 30, false)
        );
        when(quizAttemptRepository.findByQuizId("q1")).thenReturn(attempts);

        double avg = quizAttemptService.computeAvgScore("q1");

        assertThat(avg).isEqualTo(20.0);
    }

    @Test
    void computeAvgScore_handlesNonRoundAverage() {
        List<QuizAttempt> attempts = List.of(
                buildAttempt("a1", "q1", "s1", 1, false),
                buildAttempt("a2", "q1", "s2", 1, false),
                buildAttempt("a3", "q1", "s3", 2, false)
        );
        when(quizAttemptRepository.findByQuizId("q1")).thenReturn(attempts);

        double avg = quizAttemptService.computeAvgScore("q1");

        assertThat(avg).isEqualTo(1.3);
    }

    @Test
    void setAnswers_invokesMongoTemplate() {
        List<Answer> answers = List.of(
                Answer.builder().questionId("q1").selectedIdx(0).correct(true).build()
        );

        quizAttemptService.setAnswers("a1", answers);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(QuizAttempt.class));
    }

    @Test
    void setAnswers_withEmptyList_invokesMongoTemplate() {
        quizAttemptService.setAnswers("a1", new ArrayList<>());

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(QuizAttempt.class));
    }

    @Test
    void deleteById_delegatesToRepository() {
        quizAttemptService.deleteById("a1");

        verify(quizAttemptRepository).deleteById("a1");
    }

    @Test
    void deleteAllByQuiz_delegatesToRepository() {
        quizAttemptService.deleteAllByQuiz("q1");

        verify(quizAttemptRepository).deleteByQuizId("q1");
    }

    @Test
    void deleteAllByCourse_delegatesToRepository() {
        quizAttemptService.deleteAllByCourse("c1");

        verify(quizAttemptRepository).deleteByCourseId("c1");
    }

    @Test
    void deleteAllByStudent_delegatesToRepository() {
        quizAttemptService.deleteAllByStudent("s1");

        verify(quizAttemptRepository).deleteByStudentId("s1");
    }

    @Test
    void hasStudentPassedQuiz_returnsTrue_whenPassed() {
        when(quizAttemptRepository.existsByStudentIdAndQuizIdAndPassed("s1", "q1", true)).thenReturn(true);

        assertThat(quizAttemptService.hasStudentPassedQuiz("s1", "q1")).isTrue();
    }

    @Test
    void hasStudentPassedQuiz_returnsFalse_whenNotPassed() {
        when(quizAttemptRepository.existsByStudentIdAndQuizIdAndPassed("s1", "q1", true)).thenReturn(false);

        assertThat(quizAttemptService.hasStudentPassedQuiz("s1", "q1")).isFalse();
    }

    @Test
    void countByStudentAndQuiz_returnsCount() {
        when(quizAttemptRepository.countByStudentIdAndQuizId("s1", "q1")).thenReturn(3L);

        assertThat(quizAttemptService.countByStudentAndQuiz("s1", "q1")).isEqualTo(3L);
    }

    @Test
    void countPassedByQuiz_returnsCount() {
        when(quizAttemptRepository.countByQuizIdAndPassed("q1", true)).thenReturn(12L);

        assertThat(quizAttemptService.countPassedByQuiz("q1")).isEqualTo(12L);
    }

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        Date attemptedAt = new Date();
        Answer answer = Answer.builder().questionId("qq1").selectedIdx(1).correct(false).build();

        QuizAttempt attempt = QuizAttempt.builder()
                .id("a1")
                .quizId("q1")
                .courseId("c1")
                .studentId("s1")
                .attemptedAt(attemptedAt)
                .score(65)
                .passed(false)
                .timeTakenSecs(400)
                .answers(List.of(answer))
                .build();

        when(quizAttemptRepository.findById("a1")).thenReturn(Optional.of(attempt));

        QuizAttemptResponse response = quizAttemptService.findById("a1");

        assertThat(response.getId()).isEqualTo("a1");
        assertThat(response.getQuizId()).isEqualTo("q1");
        assertThat(response.getCourseId()).isEqualTo("c1");
        assertThat(response.getStudentId()).isEqualTo("s1");
        assertThat(response.getAttemptedAt()).isEqualTo(attemptedAt);
        assertThat(response.getScore()).isEqualTo(65);
        assertThat(response.isPassed()).isFalse();
        assertThat(response.getTimeTakenSecs()).isEqualTo(400);
        assertThat(response.getAnswers()).hasSize(1);
    }
}
