package ro.fiismart.chat.tools;

import org.junit.jupiter.api.Test;
import ro.fiismart.ai.dto.response.GeneratedCourseDTO;
import ro.fiismart.ai.dto.response.GeneratedLecture;
import ro.fiismart.ai.dto.response.GeneratedModule;
import ro.fiismart.ai.dto.response.GeneratedQuiz;
import ro.fiismart.ai.dto.response.GeneratedQuizQuestion;
import ro.fiismart.ai.service.CourseContentAiService;
import ro.fiismart.courses.dto.request.CreateCourseRequest;
import ro.fiismart.courses.dto.request.CreateLectureRequest;
import ro.fiismart.courses.dto.request.CreateModuleRequest;
import ro.fiismart.courses.dto.response.CourseResponse;
import ro.fiismart.courses.dto.response.LectureResponse;
import ro.fiismart.courses.dto.response.ModuleResponse;
import ro.fiismart.courses.service.CourseManagementService;
import ro.fiismart.quiz.dto.modulequiz.CreateModuleQuizRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizResponse;
import ro.fiismart.quiz.service.ModuleQuizService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CourseBuildOrchestrator}.
 *
 * <p>What we lock in:
 * <ol>
 *   <li><b>Persistence order:</b> the orchestrator persists in the
 *       sequence course → module → lectures → quiz (per module), and
 *       counts the result correctly in {@link BuildCourseResult}.</li>
 *   <li><b>Progress streaming:</b> at least one {@link ToolProgressEvent}
 *       is emitted at the start, after the course is created, after
 *       each module + lecture + quiz, and a final "Gata" event. Step
 *       counts are monotonically non-decreasing.</li>
 *   <li><b>Consumer-throw isolation:</b> if the caller's onProgress
 *       throws, persistence still completes.</li>
 * </ol>
 */
class CourseBuildOrchestratorTest {

    private static final String USER_ID = "teacher-1";
    private static final String COURSE_ID = "course-1";

    private CourseBuildOrchestrator newOrchestrator(CourseContentAiService ai,
                                                    CourseManagementService cms,
                                                    ModuleQuizService quiz) {
        return new CourseBuildOrchestrator(ai, cms, quiz);
    }

    private static GeneratedCourseDTO sampleCourse() {
        GeneratedLecture l1 = new GeneratedLecture("L1", "content 1", 300);
        GeneratedLecture l2 = new GeneratedLecture("L2", "content 2", 240);
        GeneratedQuizQuestion q1 = new GeneratedQuizQuestion(
                "Q1?", "multiple_choice",
                List.of("a", "b", "c", "d"), 1, "explanation");
        GeneratedQuiz quiz = new GeneratedQuiz("Quiz Modul A", 70, List.of(q1));
        GeneratedModule m1 = new GeneratedModule("Modul A", "desc A", List.of(l1, l2), quiz);
        return new GeneratedCourseDTO(
                "Curs Test", "desc curs", "ro", List.of("ai", "test"), List.of(m1));
    }

    @Test
    void build_happyPath_persistsCourseModuleLecturesQuizAndEmitsProgress() {
        CourseContentAiService ai = mock(CourseContentAiService.class);
        CourseManagementService cms = mock(CourseManagementService.class);
        ModuleQuizService quizSvc = mock(ModuleQuizService.class);

        when(ai.generate(anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(sampleCourse());

        // Stub createCourse to return a course with a known id.
        CourseResponse courseResp = CourseResponse.builder()
                .id(COURSE_ID).title("Curs Test").teacherId(USER_ID).status("draft").build();
        when(cms.createCourse(any(CreateCourseRequest.class))).thenReturn(courseResp);

        // Each addModule call yields a fresh moduleId.
        AtomicReference<String> lastModuleId = new AtomicReference<>();
        when(cms.addModule(eq(COURSE_ID), any(CreateModuleRequest.class)))
                .thenAnswer(inv -> {
                    String mid = UUID.randomUUID().toString();
                    lastModuleId.set(mid);
                    return ModuleResponse.builder()
                            .id(mid).title("Modul A").order(0).build();
                });

        // Lectures: dummy IDs.
        when(cms.addLectureToModule(eq(COURSE_ID), anyString(), any(CreateLectureRequest.class)))
                .thenAnswer(inv -> LectureResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .moduleId(inv.getArgument(1))
                        .title("L?")
                        .build());

        when(quizSvc.createOrUpdateModuleQuiz(eq(COURSE_ID), anyString(), any(CreateModuleQuizRequest.class)))
                .thenAnswer(inv -> ModuleQuizResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .courseId(COURSE_ID)
                        .moduleId(inv.getArgument(1))
                        .title("Quiz Modul A")
                        .build());

        CourseBuildOrchestrator orchestrator = newOrchestrator(ai, cms, quizSvc);

        List<ToolProgressEvent> events = new ArrayList<>();
        Consumer<ToolProgressEvent> recorder = events::add;

        BuildCourseSpec spec = new BuildCourseSpec(
                "AI 101", "studenti", 1, 2, 1, true, "ro");
        BuildCourseResult result = orchestrator.build(spec, USER_ID, recorder);

        // ── Result shape ────────────────────────────────────────────
        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.title()).isEqualTo("Curs Test");
        assertThat(result.moduleCount()).isEqualTo(1);
        assertThat(result.lectureCount()).isEqualTo(2);
        assertThat(result.quizCount()).isEqualTo(1);

        // ── Persistence order: course → module → lectures → quiz ───
        verify(cms, times(1)).createCourse(any(CreateCourseRequest.class));
        verify(cms, times(1)).addModule(eq(COURSE_ID), any(CreateModuleRequest.class));
        verify(cms, times(2)).addLectureToModule(eq(COURSE_ID), anyString(), any(CreateLectureRequest.class));
        verify(quizSvc, times(1))
                .createOrUpdateModuleQuiz(eq(COURSE_ID), anyString(), any(CreateModuleQuizRequest.class));

        // ── Progress: events present and monotonic ──────────────────
        assertThat(events).isNotEmpty();
        // First event: pre-generate
        assertThat(events.get(0).message()).contains("Generez");
        // Last event: terminal
        assertThat(events.get(events.size() - 1).message()).isEqualTo("Gata");
        // Step counters non-decreasing
        int prev = -1;
        for (ToolProgressEvent ev : events) {
            assertThat(ev.step()).isGreaterThanOrEqualTo(prev);
            prev = ev.step();
        }
        // Messages we expect to find across the stream
        assertThat(events).anyMatch(e -> e.message().startsWith("Structura generata"));
        assertThat(events).anyMatch(e -> e.message().startsWith("Curs creat"));
        assertThat(events).anyMatch(e -> e.message().startsWith("Modul "));
        assertThat(events).anyMatch(e -> e.message().startsWith("Lectie:"));
        assertThat(events).anyMatch(e -> e.message().startsWith("Quiz pentru:"));
    }

    @Test
    void build_progressConsumerThrows_persistenceStillCompletes() {
        CourseContentAiService ai = mock(CourseContentAiService.class);
        CourseManagementService cms = mock(CourseManagementService.class);
        ModuleQuizService quizSvc = mock(ModuleQuizService.class);

        when(ai.generate(anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(sampleCourse());

        when(cms.createCourse(any(CreateCourseRequest.class))).thenReturn(
                CourseResponse.builder().id(COURSE_ID).title("Curs Test").teacherId(USER_ID).status("draft").build());
        when(cms.addModule(eq(COURSE_ID), any(CreateModuleRequest.class))).thenReturn(
                ModuleResponse.builder().id("m-1").title("Modul A").order(0).build());
        when(cms.addLectureToModule(eq(COURSE_ID), anyString(), any(CreateLectureRequest.class)))
                .thenReturn(LectureResponse.builder().id("l-1").moduleId("m-1").title("L?").build());
        when(quizSvc.createOrUpdateModuleQuiz(eq(COURSE_ID), anyString(), any(CreateModuleQuizRequest.class)))
                .thenReturn(ModuleQuizResponse.builder().id("q-1").courseId(COURSE_ID).moduleId("m-1").title("Quiz").build());

        CourseBuildOrchestrator orchestrator = newOrchestrator(ai, cms, quizSvc);

        // Sabotage every progress callback — orchestrator must swallow.
        Consumer<ToolProgressEvent> exploding = ev -> {
            throw new RuntimeException("client gone");
        };

        BuildCourseSpec spec = new BuildCourseSpec(
                "AI 101", "studenti", 1, 2, 1, true, "ro");
        BuildCourseResult result = orchestrator.build(spec, USER_ID, exploding);

        // Persistence completed despite the exploding consumer.
        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        verify(cms, times(2)).addLectureToModule(eq(COURSE_ID), anyString(), any(CreateLectureRequest.class));
        verify(quizSvc, times(1)).createOrUpdateModuleQuiz(eq(COURSE_ID), anyString(), any(CreateModuleQuizRequest.class));
    }
}
