package com.app.service;

import com.app.dto.request.QuizQuestionRequest;
import com.app.dto.request.QuizRequest;
import com.app.dto.response.QuizQuestionResponse;
import com.app.dto.response.QuizResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Quiz;
import com.app.model.QuizQuestion;
import com.app.repository.QuizRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private QuizService quizService;

    private Quiz buildQuiz(String id, String courseId, String title) {
        return Quiz.builder()
                .id(id)
                .courseId(courseId)
                .title(title)
                .passingScore(70)
                .timeLimit(60)
                .shuffleQuestions(false)
                .questions(new ArrayList<>())
                .build();
    }

    @Test
    void create_savesQuizAndReturnsResponse() {
        QuizRequest request = QuizRequest.builder()
                .courseId("c1")
                .title("Quiz 1")
                .passingScore(70)
                .timeLimit(30)
                .shuffleQuestions(true)
                .build();

        Quiz saved = buildQuiz("q1", "c1", "Quiz 1");
        saved.setPassingScore(70);
        saved.setTimeLimit(30);
        saved.setShuffleQuestions(true);
        when(quizRepository.save(any(Quiz.class))).thenReturn(saved);

        QuizResponse response = quizService.create(request);

        assertThat(response.getId()).isEqualTo("q1");
        assertThat(response.getCourseId()).isEqualTo("c1");
        assertThat(response.getTitle()).isEqualTo("Quiz 1");
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    void create_initializesEmptyQuestionsList() {
        QuizRequest request = QuizRequest.builder()
                .courseId("c1")
                .title("Quiz")
                .passingScore(60)
                .timeLimit(20)
                .build();

        Quiz saved = buildQuiz("q1", "c1", "Quiz");
        when(quizRepository.save(any(Quiz.class))).thenReturn(saved);

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        quizService.create(request);
        verify(quizRepository).save(captor.capture());

        assertThat(captor.getValue().getQuestions()).isNotNull().isEmpty();
    }

    @Test
    void create_setsShuffleQuestionsFromRequest() {
        QuizRequest request = QuizRequest.builder()
                .courseId("c1")
                .title("Quiz")
                .passingScore(50)
                .timeLimit(15)
                .shuffleQuestions(true)
                .build();

        Quiz saved = buildQuiz("q1", "c1", "Quiz");
        saved.setShuffleQuestions(true);
        when(quizRepository.save(any(Quiz.class))).thenReturn(saved);

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        quizService.create(request);
        verify(quizRepository).save(captor.capture());

        assertThat(captor.getValue().isShuffleQuestions()).isTrue();
    }

    @Test
    void findById_returnsResponse_whenFound() {
        Quiz quiz = buildQuiz("q1", "c1", "Quiz 1");
        when(quizRepository.findById("q1")).thenReturn(Optional.of(quiz));

        QuizResponse response = quizService.findById("q1");

        assertThat(response.getId()).isEqualTo("q1");
        assertThat(response.getTitle()).isEqualTo("Quiz 1");
    }

    @Test
    void findById_throwsResourceNotFoundException_whenNotFound() {
        when(quizRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Quiz")
                .hasMessageContaining("missing");
    }

    @Test
    void findByCourseId_returnsResponse_whenFound() {
        Quiz quiz = buildQuiz("q1", "c1", "Quiz");
        when(quizRepository.findByCourseId("c1")).thenReturn(Optional.of(quiz));

        QuizResponse response = quizService.findByCourseId("c1");

        assertThat(response.getCourseId()).isEqualTo("c1");
    }

    @Test
    void findByCourseId_throwsResourceNotFoundException_whenNotFound() {
        when(quizRepository.findByCourseId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.findByCourseId("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void findQuestions_returnsQuestions_whenQuizFound() {
        QuizQuestion q = QuizQuestion.builder()
                .id("qq1")
                .text("What is Java?")
                .type("single")
                .points(10)
                .options(List.of("A lang", "A drink"))
                .correctIdx(0)
                .explanation("Java is a programming language")
                .build();

        Quiz quiz = buildQuiz("q1", "c1", "Quiz");
        quiz.setQuestions(List.of(q));
        when(quizRepository.findById("q1")).thenReturn(Optional.of(quiz));

        List<QuizQuestionResponse> questions = quizService.findQuestions("q1");

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getId()).isEqualTo("qq1");
        assertThat(questions.get(0).getText()).isEqualTo("What is Java?");
    }

    @Test
    void findQuestions_returnsEmptyList_whenQuizNotFound() {
        when(quizRepository.findById("missing")).thenReturn(Optional.empty());

        List<QuizQuestionResponse> questions = quizService.findQuestions("missing");

        assertThat(questions).isEmpty();
    }

    @Test
    void findQuestions_returnsEmptyList_whenQuizHasNoQuestions() {
        Quiz quiz = buildQuiz("q1", "c1", "Quiz");
        quiz.setQuestions(new ArrayList<>());
        when(quizRepository.findById("q1")).thenReturn(Optional.of(quiz));

        List<QuizQuestionResponse> questions = quizService.findQuestions("q1");

        assertThat(questions).isEmpty();
    }

    @Test
    void updateTitle_invokesMongoTemplate() {
        quizService.updateTitle("q1", "New Title");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    @Test
    void updatePassingScore_invokesMongoTemplate() {
        quizService.updatePassingScore("q1", 80);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    @Test
    void updateTimeLimit_invokesMongoTemplate() {
        quizService.updateTimeLimit("q1", 45);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    @Test
    void updateShuffleQuestions_true_invokesMongoTemplate() {
        quizService.updateShuffleQuestions("q1", true);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    @Test
    void updateShuffleQuestions_false_invokesMongoTemplate() {
        quizService.updateShuffleQuestions("q1", false);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    @Test
    void addQuestion_withOptions_returnsQuestionResponse() {
        QuizQuestionRequest request = QuizQuestionRequest.builder()
                .text("What is OOP?")
                .type("single")
                .points(5)
                .options(List.of("Option A", "Option B"))
                .correctIdx(0)
                .explanation("OOP is a paradigm")
                .build();

        QuizQuestionResponse response = quizService.addQuestion("q1", request);

        assertThat(response.getText()).isEqualTo("What is OOP?");
        assertThat(response.getType()).isEqualTo("single");
        assertThat(response.getPoints()).isEqualTo(5);
        assertThat(response.getOptions()).containsExactly("Option A", "Option B");
        assertThat(response.getCorrectIdx()).isEqualTo(0);
        assertThat(response.getExplanation()).isEqualTo("OOP is a paradigm");
        assertThat(response.getId()).isNotNull();
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    @Test
    void addQuestion_withNullOptions_initializesEmptyList() {
        QuizQuestionRequest request = QuizQuestionRequest.builder()
                .text("Question?")
                .type("single")
                .points(1)
                .options(null)
                .correctIdx(0)
                .build();

        QuizQuestionResponse response = quizService.addQuestion("q1", request);

        assertThat(response.getOptions()).isNotNull().isEmpty();
    }

    @Test
    void addQuestion_generatesUniqueIds() {
        QuizQuestionRequest request = QuizQuestionRequest.builder()
                .text("Q1")
                .type("single")
                .points(1)
                .options(List.of("A"))
                .correctIdx(0)
                .build();

        QuizQuestionResponse r1 = quizService.addQuestion("q1", request);
        QuizQuestionResponse r2 = quizService.addQuestion("q1", request);

        assertThat(r1.getId()).isNotEqualTo(r2.getId());
    }

    @Test
    void removeQuestion_invokesMongoTemplate() {
        quizService.removeQuestion("q1", "qq1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    @Test
    void updateQuestionField_invokesMongoTemplate() {
        quizService.updateQuestionField("q1", "qq1", "points", 20);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Quiz.class));
    }

    @Test
    void deleteById_delegatesToRepository() {
        quizService.deleteById("q1");

        verify(quizRepository).deleteById("q1");
    }

    @Test
    void deleteByCourseId_delegatesToRepository() {
        quizService.deleteByCourseId("c1");

        verify(quizRepository).deleteByCourseId("c1");
    }

    @Test
    void existsByCourseId_returnsTrue_whenExists() {
        when(quizRepository.existsByCourseId("c1")).thenReturn(true);

        assertThat(quizService.existsByCourseId("c1")).isTrue();
    }

    @Test
    void existsByCourseId_returnsFalse_whenNotExists() {
        when(quizRepository.existsByCourseId("missing")).thenReturn(false);

        assertThat(quizService.existsByCourseId("missing")).isFalse();
    }

    @Test
    void toResponse_withNullQuestions_returnsEmptyQuestionsList() {
        Quiz quiz = Quiz.builder()
                .id("q1")
                .courseId("c1")
                .title("Quiz")
                .passingScore(70)
                .timeLimit(30)
                .shuffleQuestions(false)
                .questions(null)
                .build();

        when(quizRepository.findById("q1")).thenReturn(Optional.of(quiz));

        QuizResponse response = quizService.findById("q1");

        assertThat(response.getQuestions()).isNotNull().isEmpty();
    }

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        Quiz quiz = Quiz.builder()
                .id("q1")
                .courseId("c1")
                .title("Final Quiz")
                .passingScore(75)
                .timeLimit(45)
                .shuffleQuestions(true)
                .questions(new ArrayList<>())
                .build();

        when(quizRepository.findById("q1")).thenReturn(Optional.of(quiz));

        QuizResponse response = quizService.findById("q1");

        assertThat(response.getId()).isEqualTo("q1");
        assertThat(response.getCourseId()).isEqualTo("c1");
        assertThat(response.getTitle()).isEqualTo("Final Quiz");
        assertThat(response.getPassingScore()).isEqualTo(75);
        assertThat(response.getTimeLimit()).isEqualTo(45);
        assertThat(response.isShuffleQuestions()).isTrue();
    }

    @Test
    void toQuestionResponse_mapsAllFieldsCorrectly() {
        QuizQuestion question = QuizQuestion.builder()
                .id("qq1")
                .text("What is polymorphism?")
                .type("single")
                .points(10)
                .options(List.of("A", "B", "C"))
                .correctIdx(2)
                .explanation("Polymorphism allows objects to take many forms")
                .build();

        Quiz quiz = buildQuiz("q1", "c1", "Quiz");
        quiz.setQuestions(List.of(question));
        when(quizRepository.findById("q1")).thenReturn(Optional.of(quiz));

        List<QuizQuestionResponse> questions = quizService.findQuestions("q1");

        assertThat(questions.get(0).getId()).isEqualTo("qq1");
        assertThat(questions.get(0).getText()).isEqualTo("What is polymorphism?");
        assertThat(questions.get(0).getType()).isEqualTo("single");
        assertThat(questions.get(0).getPoints()).isEqualTo(10);
        assertThat(questions.get(0).getOptions()).containsExactly("A", "B", "C");
        assertThat(questions.get(0).getCorrectIdx()).isEqualTo(2);
        assertThat(questions.get(0).getExplanation()).isEqualTo("Polymorphism allows objects to take many forms");
    }
}
