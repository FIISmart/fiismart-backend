package ro.fiismart.quiz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.ModuleQuiz;
import ro.fiismart.common.model.ModuleQuizQuestion;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.ModuleQuizRepository;
import ro.fiismart.quiz.dto.modulequiz.CreateModuleQuizRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizQuestionRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleQuizServiceTest {

    @Mock
    private ModuleQuizRepository moduleQuizRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ModuleQuizService moduleQuizService;

    private ModuleQuiz sampleQuiz;
    private CreateModuleQuizRequest createReq;

    @BeforeEach
    void setUp() {
        ModuleQuizQuestion q1 = ModuleQuizQuestion.builder()
                .id("q1").text("Intrebare 1").type("multiple_choice").points(5)
                .options(List.of("A", "B", "C")).correctIdx(0).build();

        sampleQuiz = ModuleQuiz.builder()
                .id("quiz1")
                .courseId("course1")
                .moduleId("mod1")
                .lectureId("lec1")
                .quizScope("lecture")
                .title("Quiz Lecție")
                .passingScore(70)
                .timeLimit(30)
                .questions(new ArrayList<>(List.of(q1)))
                .build();

        createReq = new CreateModuleQuizRequest();
        createReq.setTitle("Quiz Nou");
        createReq.setPassingScore(60);
        createReq.setTimeLimit(20);
        createReq.setQuestions(null);
    }

    // ── getAllQuizzesByCourse ──────────────────────────────────────────────────

    @Test
    void getAllQuizzesByCourse_courseExists_returnsList() {
        when(courseRepository.existsById("course1")).thenReturn(true);
        when(moduleQuizRepository.findAllByCourseId("course1")).thenReturn(List.of(sampleQuiz));

        List<ModuleQuizResponse> result = moduleQuizService.getAllQuizzesByCourse("course1");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllQuizzesByCourse_courseNotFound_throws() {
        when(courseRepository.existsById("noId")).thenReturn(false);

        assertThatThrownBy(() -> moduleQuizService.getAllQuizzesByCourse("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── LECTURE QUIZ ──────────────────────────────────────────────────────────

    @Test
    void createOrUpdateLectureQuiz_savesAndReturns() {
        when(courseRepository.existsById("course1")).thenReturn(true);
        when(moduleQuizRepository.save(any(ModuleQuiz.class))).thenReturn(sampleQuiz);

        ModuleQuizResponse result = moduleQuizService.createOrUpdateLectureQuiz(
                "course1", "mod1", "lec1", createReq);

        assertThat(result).isNotNull();
        verify(moduleQuizRepository).deleteByLectureIdAndQuizScope("lec1", "lecture");
        verify(mongoTemplate).updateFirst(any(), any(), eq(Course.class));
    }

    @Test
    void getLectureQuiz_found_returnsResponse() {
        when(moduleQuizRepository.findByLectureIdAndQuizScope("lec1", "lecture"))
                .thenReturn(Optional.of(sampleQuiz));

        ModuleQuizResponse result = moduleQuizService.getLectureQuiz("course1", "mod1", "lec1");

        assertThat(result).isNotNull();
    }

    @Test
    void getLectureQuiz_notFound_throws() {
        when(moduleQuizRepository.findByLectureIdAndQuizScope("noLec", "lecture"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleQuizService.getLectureQuiz("course1", "mod1", "noLec"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteLectureQuiz_existsAndDeletes() {
        when(moduleQuizRepository.existsByLectureIdAndQuizScope("lec1", "lecture")).thenReturn(true);

        moduleQuizService.deleteLectureQuiz("course1", "mod1", "lec1");

        verify(moduleQuizRepository).deleteByLectureIdAndQuizScope("lec1", "lecture");
    }

    @Test
    void deleteLectureQuiz_notFound_throws() {
        when(moduleQuizRepository.existsByLectureIdAndQuizScope("noLec", "lecture")).thenReturn(false);

        assertThatThrownBy(() -> moduleQuizService.deleteLectureQuiz("course1", "mod1", "noLec"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addQuestionToLectureQuiz_appendsQuestion() {
        ModuleQuizQuestionRequest req = new ModuleQuizQuestionRequest();
        req.setText("Noua intrebare");
        req.setType("multiple_choice");
        req.setPoints(10);

        when(moduleQuizRepository.findByLectureIdAndQuizScope("lec1", "lecture"))
                .thenReturn(Optional.of(sampleQuiz));
        when(moduleQuizRepository.findById("quiz1")).thenReturn(Optional.of(sampleQuiz));

        ModuleQuizResponse result = moduleQuizService.addQuestionToLectureQuiz(
                "course1", "mod1", "lec1", req);

        assertThat(result).isNotNull();
        verify(mongoTemplate, atLeastOnce()).updateFirst(any(), any(), any(Class.class));
    }

    @Test
    void removeQuestionFromLectureQuiz_questionExists_removes() {
        when(moduleQuizRepository.findByLectureIdAndQuizScope("lec1", "lecture"))
                .thenReturn(Optional.of(sampleQuiz));

        moduleQuizService.removeQuestionFromLectureQuiz("course1", "mod1", "lec1", "q1");

        verify(mongoTemplate).updateFirst(any(), any(), eq(ModuleQuiz.class));
    }

    @Test
    void removeQuestionFromLectureQuiz_questionNotFound_throws() {
        when(moduleQuizRepository.findByLectureIdAndQuizScope("lec1", "lecture"))
                .thenReturn(Optional.of(sampleQuiz));

        assertThatThrownBy(() ->
                moduleQuizService.removeQuestionFromLectureQuiz("course1", "mod1", "lec1", "noQ"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reorderLectureQuizQuestions_validOrder_reorders() {
        when(moduleQuizRepository.findByLectureIdAndQuizScope("lec1", "lecture"))
                .thenReturn(Optional.of(sampleQuiz));
        when(moduleQuizRepository.findById("quiz1")).thenReturn(Optional.of(sampleQuiz));

        moduleQuizService.reorderLectureQuizQuestions("course1", "mod1", "lec1", List.of("q1"));

        verify(mongoTemplate, atLeastOnce()).updateFirst(any(), any(), any(Class.class));
    }

    @Test
    void reorderLectureQuizQuestions_wrongCount_throws() {
        when(moduleQuizRepository.findByLectureIdAndQuizScope("lec1", "lecture"))
                .thenReturn(Optional.of(sampleQuiz));

        assertThatThrownBy(() ->
                moduleQuizService.reorderLectureQuizQuestions("course1", "mod1", "lec1",
                        List.of("q1", "q2")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── MODULE QUIZ ───────────────────────────────────────────────────────────

    @Test
    void createOrUpdateModuleQuiz_savesAndReturns() {
        when(courseRepository.existsById("course1")).thenReturn(true);
        when(moduleQuizRepository.save(any(ModuleQuiz.class))).thenReturn(sampleQuiz);

        ModuleQuizResponse result = moduleQuizService.createOrUpdateModuleQuiz("course1", "mod1", createReq);

        assertThat(result).isNotNull();
        verify(moduleQuizRepository).deleteByModuleIdAndQuizScope("mod1", "module");
    }

    @Test
    void getModuleQuiz_found_returnsResponse() {
        ModuleQuiz moduleQuiz = ModuleQuiz.builder().id("q2").moduleId("mod1").quizScope("module").build();
        when(moduleQuizRepository.findByModuleIdAndQuizScope("mod1", "module"))
                .thenReturn(Optional.of(moduleQuiz));

        ModuleQuizResponse result = moduleQuizService.getModuleQuiz("course1", "mod1");

        assertThat(result).isNotNull();
    }

    @Test
    void deleteModuleQuiz_existsAndDeletes() {
        when(moduleQuizRepository.existsByModuleIdAndQuizScope("mod1", "module")).thenReturn(true);

        moduleQuizService.deleteModuleQuiz("course1", "mod1");

        verify(moduleQuizRepository).deleteByModuleIdAndQuizScope("mod1", "module");
    }

    @Test
    void deleteModuleQuiz_notFound_throws() {
        when(moduleQuizRepository.existsByModuleIdAndQuizScope("noMod", "module")).thenReturn(false);

        assertThatThrownBy(() -> moduleQuizService.deleteModuleQuiz("course1", "noMod"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── COURSE FINAL QUIZ ─────────────────────────────────────────────────────

    @Test
    void createOrUpdateCourseFinalQuiz_savesAndReturns() {
        when(courseRepository.existsById("course1")).thenReturn(true);
        when(moduleQuizRepository.save(any(ModuleQuiz.class))).thenReturn(sampleQuiz);

        ModuleQuizResponse result = moduleQuizService.createOrUpdateCourseFinalQuiz("course1", createReq);

        assertThat(result).isNotNull();
        verify(moduleQuizRepository).deleteByCourseIdAndQuizScope("course1", "course_final");
    }

    @Test
    void getCourseFinalQuiz_found_returnsResponse() {
        ModuleQuiz finalQuiz = ModuleQuiz.builder().id("fq1").courseId("course1")
                .quizScope("course_final").build();
        when(moduleQuizRepository.findByCourseIdAndQuizScope("course1", "course_final"))
                .thenReturn(Optional.of(finalQuiz));

        ModuleQuizResponse result = moduleQuizService.getCourseFinalQuiz("course1");

        assertThat(result).isNotNull();
    }

    @Test
    void deleteCourseFinalQuiz_existsAndDeletes() {
        when(moduleQuizRepository.existsByCourseIdAndQuizScope("course1", "course_final")).thenReturn(true);

        moduleQuizService.deleteCourseFinalQuiz("course1");

        verify(moduleQuizRepository).deleteByCourseIdAndQuizScope("course1", "course_final");
    }

    @Test
    void deleteCourseFinalQuiz_notFound_throws() {
        when(moduleQuizRepository.existsByCourseIdAndQuizScope("noId", "course_final")).thenReturn(false);

        assertThatThrownBy(() -> moduleQuizService.deleteCourseFinalQuiz("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrUpdateLectureQuiz_withQuestions_buildsQuestions() {
        ModuleQuizQuestionRequest qReq = new ModuleQuizQuestionRequest();
        qReq.setText("Intrebare cu optiuni");
        qReq.setType("multiple_choice");
        qReq.setPoints(5);
        qReq.setOptions(List.of("A", "B"));
        qReq.setCorrectIdx(0);

        createReq.setQuestions(List.of(qReq));

        when(courseRepository.existsById("course1")).thenReturn(true);
        when(moduleQuizRepository.save(any(ModuleQuiz.class))).thenReturn(sampleQuiz);

        ModuleQuizResponse result = moduleQuizService.createOrUpdateLectureQuiz(
                "course1", "mod1", "lec1", createReq);

        assertThat(result).isNotNull();
    }
}
