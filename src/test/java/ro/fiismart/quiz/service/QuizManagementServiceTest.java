package ro.fiismart.quiz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Quiz;
import ro.fiismart.common.model.QuizQuestion;
import ro.fiismart.common.repository.QuizRepository;
import ro.fiismart.quiz.dto.QuizQuestionRequest;
import ro.fiismart.quiz.dto.QuizQuestionResponse;
import ro.fiismart.quiz.dto.QuizRequest;
import ro.fiismart.quiz.dto.QuizResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizManagementServiceTest {

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private QuizManagementService quizManagementService;

    private Quiz sampleQuiz;

    @BeforeEach
    void setUp() {
        QuizQuestion q1 = QuizQuestion.builder()
                .id("q1").text("2+2=?").type("multiple_choice").points(10)
                .options(List.of("3", "4", "5")).correctIdx(1).build();

        sampleQuiz = Quiz.builder()
                .id("quiz1")
                .courseId("course1")
                .title("Quiz Matematică")
                .passingScore(70)
                .timeLimit(60)
                .shuffleQuestions(false)
                .questions(new ArrayList<>(List.of(q1)))
                .build();
    }

    @Test
    void create_savesAndReturnsResponse() {
        QuizRequest req = new QuizRequest();
        req.setCourseId("course1");
        req.setTitle("Quiz nou");
        req.setPassingScore(60);
        req.setTimeLimit(30);

        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);

        QuizResponse result = quizManagementService.create(req);

        assertThat(result.getId()).isEqualTo("quiz1");
        assertThat(result.getTitle()).isEqualTo("Quiz Matematică");
    }

    @Test
    void findById_found_returnsResponse() {
        when(quizRepository.findById("quiz1")).thenReturn(Optional.of(sampleQuiz));

        QuizResponse result = quizManagementService.findById("quiz1");

        assertThat(result.getId()).isEqualTo("quiz1");
    }

    @Test
    void findById_notFound_throws() {
        when(quizRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizManagementService.findById("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByCourseId_found_returnsResponse() {
        when(quizRepository.findByCourseId("course1")).thenReturn(Optional.of(sampleQuiz));

        QuizResponse result = quizManagementService.findByCourseId("course1");

        assertThat(result.getCourseId()).isEqualTo("course1");
    }

    @Test
    void findByCourseId_notFound_throws() {
        when(quizRepository.findByCourseId("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizManagementService.findByCourseId("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findQuestions_found_returnsList() {
        when(quizRepository.findById("quiz1")).thenReturn(Optional.of(sampleQuiz));

        List<QuizQuestionResponse> result = quizManagementService.findQuestions("quiz1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("2+2=?");
    }

    @Test
    void findQuestions_notFound_returnsEmpty() {
        when(quizRepository.findById("noId")).thenReturn(Optional.empty());

        List<QuizQuestionResponse> result = quizManagementService.findQuestions("noId");

        assertThat(result).isEmpty();
    }

    @Test
    void updateTitle_callsMongoTemplate() {
        quizManagementService.updateTitle("quiz1", "Nou titlu");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Quiz.class));
    }

    @Test
    void updatePassingScore_callsMongoTemplate() {
        quizManagementService.updatePassingScore("quiz1", 80);

        verify(mongoTemplate).updateFirst(any(), any(), eq(Quiz.class));
    }

    @Test
    void updateTimeLimit_callsMongoTemplate() {
        quizManagementService.updateTimeLimit("quiz1", 45);

        verify(mongoTemplate).updateFirst(any(), any(), eq(Quiz.class));
    }

    @Test
    void addQuestion_buildsAndPushesQuestion() {
        QuizQuestionRequest req = new QuizQuestionRequest();
        req.setText("Ce este Pi?");
        req.setType("multiple_choice");
        req.setPoints(5);
        req.setOptions(List.of("3.14", "2.71", "1.41"));
        req.setCorrectIdx(0);
        req.setExplanation("Pi ≈ 3.14");

        QuizQuestionResponse result = quizManagementService.addQuestion("quiz1", req);

        assertThat(result.getText()).isEqualTo("Ce este Pi?");
        verify(mongoTemplate).updateFirst(any(), any(), eq(Quiz.class));
    }

    @Test
    void addQuestion_nullOptions_usesEmptyList() {
        QuizQuestionRequest req = new QuizQuestionRequest();
        req.setText("Intrebare deschisa");
        req.setType("open");
        req.setPoints(10);
        req.setOptions(null);

        QuizQuestionResponse result = quizManagementService.addQuestion("quiz1", req);

        assertThat(result).isNotNull();
    }

    @Test
    void removeQuestion_callsMongoTemplate() {
        quizManagementService.removeQuestion("quiz1", "q1");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Quiz.class));
    }

    @Test
    void deleteById_callsRepository() {
        quizManagementService.deleteById("quiz1");

        verify(quizRepository).deleteById("quiz1");
    }

    @Test
    void deleteByCourseId_callsRepository() {
        quizManagementService.deleteByCourseId("course1");

        verify(quizRepository).deleteByCourseId("course1");
    }

    @Test
    void existsByCourseId_delegatesToRepository() {
        when(quizRepository.existsByCourseId("course1")).thenReturn(true);

        assertThat(quizManagementService.existsByCourseId("course1")).isTrue();
    }
}
