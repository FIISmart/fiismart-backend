package ro.fiismart.chat.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
import ro.fiismart.courses.dto.response.ModuleResponse;
import ro.fiismart.courses.service.CourseManagementService;
import ro.fiismart.quiz.dto.modulequiz.CreateModuleQuizRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizQuestionRequest;
import ro.fiismart.quiz.service.ModuleQuizService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wires together {@link CourseContentAiService} (generate) and the
 * existing course / quiz persistence services (write) into a single
 * orchestrated multi-step build, emitting a {@link ToolProgressEvent}
 * after each meaningful action so the chat UI can render a streaming
 * checklist.
 *
 * <p><b>Crash policy:</b> on any per-step failure we log + emit one
 * final error progress event + rethrow. We do <em>not</em> roll back —
 * partial progress in a {@code draft} course is more useful to the
 * professor than a silent zero. The chat reply (driven by the model's
 * follow-up turn) will still inform the user.
 *
 * <p><b>onProgress contract:</b> caller passes a non-null consumer.
 * If the consumer throws (e.g. SSE emit fails because the client
 * disconnected), the orchestrator catches and swallows the throw —
 * progress is best-effort; we keep persisting either way.
 */
@Service
public class CourseBuildOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CourseBuildOrchestrator.class);

    static final String TOOL_NAME = "buildFullCourse";

    /** Default values applied when the model omits a per-lecture estimate. */
    private static final int DEFAULT_LECTURE_DURATION_SECS = 300;
    /** Default quiz pass threshold when the model omits one. */
    private static final int DEFAULT_QUIZ_PASSING_SCORE = 70;

    private final CourseContentAiService aiService;
    private final CourseManagementService courseManagementService;
    private final ModuleQuizService moduleQuizService;

    public CourseBuildOrchestrator(CourseContentAiService aiService,
                                   CourseManagementService courseManagementService,
                                   ModuleQuizService moduleQuizService) {
        this.aiService = aiService;
        this.courseManagementService = courseManagementService;
        this.moduleQuizService = moduleQuizService;
    }

    /**
     * Generates a course tree and persists it module-by-module, lecture-
     * by-lecture, quiz-by-quiz. Returns counts of what was actually
     * persisted (may be less than spec on partial failure — but in that
     * case we rethrow rather than return the partial result).
     */
    public BuildCourseResult build(BuildCourseSpec spec,
                                   String userId,
                                   Consumer<ToolProgressEvent> onProgress) {

        Consumer<ToolProgressEvent> safeProgress = wrap(onProgress);

        // Total = generate(1) + persist course(1) + per-module (1 module
        // event + N lectures + maybe 1 quiz). We don't know exact module
        // counts until the model replies so step 1's total is an upper
        // bound based on spec; we re-emit total after generate returns.
        int approxTotal = 2 + spec.moduleCount() * (1 + spec.lecturesPerModule()
                + (spec.includeQuizzes() ? 1 : 0));
        int step = 0;
        safeProgress.accept(new ToolProgressEvent(
                TOOL_NAME, step, approxTotal, "Generez structura cursului..."));

        GeneratedCourseDTO dto;
        try {
            dto = aiService.generate(
                    spec.subject(), spec.audience(),
                    spec.moduleCount(), spec.lecturesPerModule(),
                    spec.questionsPerQuiz(), spec.includeQuizzes(),
                    spec.language());
        } catch (RuntimeException e) {
            log.warn("CourseBuildOrchestrator: generate failed: {}", e.getClass().getSimpleName());
            safeProgress.accept(new ToolProgressEvent(
                    TOOL_NAME, step, approxTotal, "Eroare la generarea structurii."));
            throw e;
        }

        // Re-compute total now that we know exact module/lecture/quiz
        // counts from the model.
        int total = computeTotal(dto, spec.includeQuizzes());
        step++;
        safeProgress.accept(new ToolProgressEvent(
                TOOL_NAME, step, total, "Structura generata: \""
                + safe(dto.title()) + "\""));

        // ── Persist course shell ────────────────────────────────────
        CreateCourseRequest courseReq = new CreateCourseRequest();
        courseReq.setTitle(safe(dto.title()));
        courseReq.setDescription(safe(dto.description()));
        courseReq.setTeacherId(userId);
        courseReq.setLanguage(dto.language() != null ? dto.language() : spec.language());
        courseReq.setTags(dto.tags() != null ? new ArrayList<>(dto.tags()) : new ArrayList<>());

        CourseResponse created;
        try {
            created = courseManagementService.createCourse(courseReq);
        } catch (RuntimeException e) {
            log.warn("CourseBuildOrchestrator: createCourse failed: {}", e.getClass().getSimpleName());
            safeProgress.accept(new ToolProgressEvent(
                    TOOL_NAME, step, total, "Eroare la crearea cursului."));
            throw e;
        }
        String courseId = created.getId();
        step++;
        safeProgress.accept(new ToolProgressEvent(
                TOOL_NAME, step, total, "Curs creat (draft)"));

        int lectureCount = 0;
        int quizCount = 0;
        int moduleCount = 0;

        // ── Persist modules / lectures / quizzes ────────────────────
        List<GeneratedModule> modules = dto.modules() != null ? dto.modules() : List.of();
        for (int m = 0; m < modules.size(); m++) {
            GeneratedModule mod = modules.get(m);
            try {
                CreateModuleRequest modReq = new CreateModuleRequest();
                modReq.setTitle(safe(mod.title()));
                modReq.setDescription(safe(mod.description()));
                ModuleResponse persistedModule =
                        courseManagementService.addModule(courseId, modReq);
                String moduleId = persistedModule.getId();
                moduleCount++;
                step++;
                safeProgress.accept(new ToolProgressEvent(
                        TOOL_NAME, step, total,
                        "Modul " + (m + 1) + "/" + modules.size() + ": "
                                + safe(mod.title())));

                // Lectures.
                List<GeneratedLecture> lectures =
                        mod.lectures() != null ? mod.lectures() : List.of();
                for (GeneratedLecture lec : lectures) {
                    CreateLectureRequest lecReq = new CreateLectureRequest();
                    lecReq.setTitle(safe(lec.title()));
                    lecReq.setType("markdown");
                    lecReq.setContent(safe(lec.content()));
                    lecReq.setDurationSecs(lec.durationSecs() != null
                            ? lec.durationSecs()
                            : DEFAULT_LECTURE_DURATION_SECS);
                    courseManagementService.addLectureToModule(courseId, moduleId, lecReq);
                    lectureCount++;
                    step++;
                    safeProgress.accept(new ToolProgressEvent(
                            TOOL_NAME, step, total,
                            "Lectie: " + safe(lec.title())));
                }

                // Quiz (only when present + requested).
                if (spec.includeQuizzes() && mod.quiz() != null) {
                    GeneratedQuiz q = mod.quiz();
                    CreateModuleQuizRequest quizReq = new CreateModuleQuizRequest();
                    quizReq.setTitle(safe(q.title()));
                    quizReq.setPassingScore(q.passingScore() != null
                            ? q.passingScore()
                            : DEFAULT_QUIZ_PASSING_SCORE);
                    quizReq.setQuestions(toQuestionRequests(q.questions()));
                    moduleQuizService.createOrUpdateModuleQuiz(courseId, moduleId, quizReq);
                    quizCount++;
                    step++;
                    safeProgress.accept(new ToolProgressEvent(
                            TOOL_NAME, step, total,
                            "Quiz pentru: " + safe(mod.title())));
                }
            } catch (RuntimeException e) {
                log.warn("CourseBuildOrchestrator: module {} failed: {}",
                        m, e.getClass().getSimpleName());
                safeProgress.accept(new ToolProgressEvent(
                        TOOL_NAME, step, total,
                        "Eroare la modul " + (m + 1) + "/" + modules.size()));
                throw e;
            }
        }

        safeProgress.accept(new ToolProgressEvent(
                TOOL_NAME, total, total, "Gata"));
        return new BuildCourseResult(courseId, safe(dto.title()),
                moduleCount, lectureCount, quizCount);
    }

    private static int computeTotal(GeneratedCourseDTO dto, boolean includeQuizzes) {
        int total = 2; // generate stage + course create
        if (dto.modules() == null) return total;
        for (GeneratedModule m : dto.modules()) {
            total += 1; // module create
            total += m.lectures() != null ? m.lectures().size() : 0;
            if (includeQuizzes && m.quiz() != null) total += 1;
        }
        return total;
    }

    private static List<ModuleQuizQuestionRequest> toQuestionRequests(
            List<GeneratedQuizQuestion> src) {
        List<ModuleQuizQuestionRequest> out = new ArrayList<>();
        if (src == null) return out;
        for (GeneratedQuizQuestion q : src) {
            ModuleQuizQuestionRequest req = new ModuleQuizQuestionRequest();
            req.setText(safe(q.text()));
            // We only persist multiple_choice in v1; if the model ever
            // emits something else, force it back to MC to stay schema-
            // compatible with the storage layer.
            req.setType("multiple_choice");
            req.setOptions(q.options() != null ? new ArrayList<>(q.options()) : new ArrayList<>());
            req.setCorrectIdx(q.correctIdx());
            req.setExplanation(safe(q.explanation()));
            out.add(req);
        }
        return out;
    }

    /** Defensive null/blank coalescing — Gemini occasionally omits
     *  optional strings even when the schema marks them required. */
    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Wraps the caller's consumer so a throw from inside it (e.g. SSE
     * emit blowing up on a disconnected client) cannot interrupt the
     * orchestrator loop. Progress is best-effort.
     */
    private static Consumer<ToolProgressEvent> wrap(Consumer<ToolProgressEvent> raw) {
        if (raw == null) return e -> { };
        return ev -> {
            try {
                raw.accept(ev);
            } catch (Exception ignored) {
                // best-effort — progress channel is decorative
            }
        };
    }
}
