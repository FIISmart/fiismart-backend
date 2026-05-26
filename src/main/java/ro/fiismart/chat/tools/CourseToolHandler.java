package ro.fiismart.chat.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.fiismart.chat.dto.ToolDispatchContext;
import ro.fiismart.chat.dto.request.RouteContextDTO;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Lecture;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.courses.dto.request.CreateLectureRequest;
import ro.fiismart.courses.dto.request.CreateModuleRequest;
import ro.fiismart.courses.dto.request.UpdateLectureRequest;
import ro.fiismart.courses.dto.response.LectureResponse;
import ro.fiismart.courses.dto.response.ModuleResponse;
import ro.fiismart.courses.service.CourseManagementService;
import ro.fiismart.quiz.dto.modulequiz.CreateModuleQuizRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizQuestionRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizResponse;
import ro.fiismart.quiz.service.ModuleQuizService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Server-side execution of the chat's modify-course tools. Every entry
 * point obeys the same four-step contract:
 *
 * <ol>
 *   <li>Pin {@code courseId} from {@code ctx.routeContext().courseId()}.
 *       Never from AI args. Reject if absent.</li>
 *   <li>Hard ownership check via
 *       {@link CourseManagementService#assertOwner}.</li>
 *   <li>Validate every AI-supplied {@code moduleId} / {@code lectureId} /
 *       {@code moduleQuizId} against the actual course tree — reject
 *       (return an error result) if any id is not found.</li>
 *   <li>Delegate to the corresponding existing service and emit a
 *       single {@link ToolProgressEvent} with the user-facing summary.</li>
 * </ol>
 *
 * <p>Errors are surfaced as a result map with an {@code error} key, not
 * by throwing — the outer {@code ChatToolHandler.dispatch} loop already
 * wraps exceptions into generic "tool indisponibil temporar" payloads
 * that lose the actionable detail. Returning a structured error lets
 * the model see exactly what went wrong (e.g. "moduleId xyz nu exista")
 * and apologize coherently to the user.
 */
@Service
public class CourseToolHandler {

    private static final Logger log = LoggerFactory.getLogger(CourseToolHandler.class);

    public static final String TOOL_ADD_MODULE = "addModule";
    public static final String TOOL_UPDATE_MODULE = "updateModule";
    public static final String TOOL_DELETE_MODULE = "deleteModule";
    public static final String TOOL_REORDER_MODULES = "reorderModules";
    public static final String TOOL_ADD_LECTURE = "addLecture";
    public static final String TOOL_UPDATE_LECTURE = "updateLecture";
    public static final String TOOL_DELETE_LECTURE = "deleteLecture";
    public static final String TOOL_REORDER_LECTURES = "reorderLectures";
    public static final String TOOL_ADD_MODULE_QUIZ = "addModuleQuiz";
    public static final String TOOL_UPDATE_MODULE_QUIZ = "updateModuleQuiz";
    public static final String TOOL_DELETE_MODULE_QUIZ = "deleteModuleQuiz";

    private static final String ERR_NO_ACTIVE_COURSE =
            "Trebuie sa fii pe pagina unui curs pentru a modifica.";

    private final CourseManagementService courseManagementService;
    private final ModuleQuizService moduleQuizService;
    private final CourseRepository courseRepository;

    public CourseToolHandler(CourseManagementService courseManagementService,
                             ModuleQuizService moduleQuizService,
                             CourseRepository courseRepository) {
        this.courseManagementService = courseManagementService;
        this.moduleQuizService = moduleQuizService;
        this.courseRepository = courseRepository;
    }

    // ── MODULE OPERATIONS ────────────────────────────────────────────

    public Map<String, Object> addModule(Map<String, Object> args, ToolDispatchContext ctx) {
        String courseId = requireActiveCourse(ctx);
        if (courseId == null) return errorNoActiveCourse();
        courseManagementService.assertOwner(courseId, ctx.userId());

        String title = requireString(args, "title");
        String description = optionalString(args, "description");

        CreateModuleRequest req = new CreateModuleRequest();
        req.setTitle(title);
        req.setDescription(description);
        ModuleResponse created = courseManagementService.addModule(courseId, req);

        emit(ctx, TOOL_ADD_MODULE, "Modul adaugat: " + title);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("moduleId", created.getId());
        out.put("title", created.getTitle());
        out.put("order", created.getOrder());
        return out;
    }

    public Map<String, Object> updateModule(Map<String, Object> args, ToolDispatchContext ctx) {
        String courseId = requireActiveCourse(ctx);
        if (courseId == null) return errorNoActiveCourse();
        courseManagementService.assertOwner(courseId, ctx.userId());

        String moduleId = requireString(args, "moduleId");
        Course course = loadCourseOrThrow(courseId);
        CourseModule existing = findModuleOrNull(course, moduleId);
        if (existing == null) return errorUnknownId("moduleId", moduleId);

        // Allow partial updates: a null/missing field on the request
        // means "keep the current value". The underlying service
        // overwrites both fields unconditionally, so we hand it the
        // pre-existing value when the AI omitted one.
        String nextTitle = optionalString(args, "title");
        String nextDescription = optionalString(args, "description");

        CreateModuleRequest req = new CreateModuleRequest();
        req.setTitle(nextTitle != null ? nextTitle : existing.getTitle());
        req.setDescription(nextDescription != null ? nextDescription : existing.getDescription());
        ModuleResponse updated = courseManagementService.updateModule(courseId, moduleId, req);

        emit(ctx, TOOL_UPDATE_MODULE, "Modul actualizat: " + updated.getTitle());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("moduleId", updated.getId());
        return out;
    }

    public Map<String, Object> deleteModule(Map<String, Object> args, ToolDispatchContext ctx) {
        String courseId = requireActiveCourse(ctx);
        if (courseId == null) return errorNoActiveCourse();
        courseManagementService.assertOwner(courseId, ctx.userId());

        String moduleId = requireString(args, "moduleId");
        Course course = loadCourseOrThrow(courseId);
        CourseModule existing = findModuleOrNull(course, moduleId);
        if (existing == null) return errorUnknownId("moduleId", moduleId);

        String moduleTitle = existing.getTitle();
        courseManagementService.deleteModule(courseId, moduleId);

        emit(ctx, TOOL_DELETE_MODULE, "Modul sters: " + moduleTitle);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("moduleId", moduleId);
        return out;
    }

    public Map<String, Object> reorderModules(Map<String, Object> args, ToolDispatchContext ctx) {
        String courseId = requireActiveCourse(ctx);
        if (courseId == null) return errorNoActiveCourse();
        courseManagementService.assertOwner(courseId, ctx.userId());

        List<String> orderedIds = requireStringList(args, "orderedIds");
        Course course = loadCourseOrThrow(courseId);
        for (String id : orderedIds) {
            if (findModuleOrNull(course, id) == null) {
                return errorUnknownId("moduleId", id);
            }
        }
        List<ModuleResponse> reordered = courseManagementService.reorderModules(courseId, orderedIds);

        emit(ctx, TOOL_REORDER_MODULES,
                "Module reordonate (" + reordered.size() + ")");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orderedIds", reordered.stream().map(ModuleResponse::getId).toList());
        return out;
    }

    // ── LECTURE OPERATIONS ───────────────────────────────────────────

    public Map<String, Object> addLecture(Map<String, Object> args, ToolDispatchContext ctx) {
        String courseId = requireActiveCourse(ctx);
        if (courseId == null) return errorNoActiveCourse();
        courseManagementService.assertOwner(courseId, ctx.userId());

        String moduleId = requireString(args, "moduleId");
        Course course = loadCourseOrThrow(courseId);
        if (findModuleOrNull(course, moduleId) == null) {
            return errorUnknownId("moduleId", moduleId);
        }

        String title = requireString(args, "title");
        String content = optionalString(args, "content");
        Integer durationSecs = optionalInt(args, "durationSecs");

        CreateLectureRequest req = new CreateLectureRequest();
        req.setTitle(title);
        req.setType("markdown");
        req.setContent(content != null ? content : "");
        req.setDurationSecs(durationSecs != null ? durationSecs : 0);
        LectureResponse created = courseManagementService.addLectureToModule(courseId, moduleId, req);

        emit(ctx, TOOL_ADD_LECTURE, "Lectie adaugata: " + title);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("lectureId", created.getId());
        return out;
    }

    public Map<String, Object> updateLecture(Map<String, Object> args, ToolDispatchContext ctx) {
        String courseId = requireActiveCourse(ctx);
        if (courseId == null) return errorNoActiveCourse();
        courseManagementService.assertOwner(courseId, ctx.userId());

        String moduleId = requireString(args, "moduleId");
        String lectureId = requireString(args, "lectureId");
        Course course = loadCourseOrThrow(courseId);
        CourseModule module = findModuleOrNull(course, moduleId);
        if (module == null) return errorUnknownId("moduleId", moduleId);
        if (findLectureOrNull(module, lectureId) == null) {
            return errorUnknownId("lectureId", lectureId);
        }

        UpdateLectureRequest req = new UpdateLectureRequest();
        req.setTitle(optionalString(args, "title"));
        req.setContent(optionalString(args, "content"));
        Integer dur = optionalInt(args, "durationSecs");
        if (dur != null) req.setDurationSecs(dur);
        LectureResponse updated = courseManagementService.updateLectureInModule(
                courseId, moduleId, lectureId, req);

        emit(ctx, TOOL_UPDATE_LECTURE, "Lectie actualizata: "
                + (updated != null ? updated.getTitle() : lectureId));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("lectureId", lectureId);
        return out;
    }

    public Map<String, Object> deleteLecture(Map<String, Object> args, ToolDispatchContext ctx) {
        String courseId = requireActiveCourse(ctx);
        if (courseId == null) return errorNoActiveCourse();
        courseManagementService.assertOwner(courseId, ctx.userId());

        String moduleId = requireString(args, "moduleId");
        String lectureId = requireString(args, "lectureId");
        Course course = loadCourseOrThrow(courseId);
        CourseModule module = findModuleOrNull(course, moduleId);
        if (module == null) return errorUnknownId("moduleId", moduleId);
        Lecture lec = findLectureOrNull(module, lectureId);
        if (lec == null) return errorUnknownId("lectureId", lectureId);

        String lectureTitle = lec.getTitle();
        courseManagementService.removeLectureFromModule(courseId, moduleId, lectureId);

        emit(ctx, TOOL_DELETE_LECTURE, "Lectie stearsa: " + lectureTitle);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("lectureId", lectureId);
        return out;
    }

    public Map<String, Object> reorderLectures(Map<String, Object> args, ToolDispatchContext ctx) {
        String courseId = requireActiveCourse(ctx);
        if (courseId == null) return errorNoActiveCourse();
        courseManagementService.assertOwner(courseId, ctx.userId());

        String moduleId = requireString(args, "moduleId");
        List<String> orderedIds = requireStringList(args, "orderedIds");
        Course course = loadCourseOrThrow(courseId);
        CourseModule module = findModuleOrNull(course, moduleId);
        if (module == null) return errorUnknownId("moduleId", moduleId);
        for (String id : orderedIds) {
            if (findLectureOrNull(module, id) == null) {
                return errorUnknownId("lectureId", id);
            }
        }
        List<LectureResponse> reordered = courseManagementService.reorderLecturesInModule(
                courseId, moduleId, orderedIds);

        emit(ctx, TOOL_REORDER_LECTURES,
                "Lectii reordonate (" + reordered.size() + ")");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orderedIds", reordered.stream().map(LectureResponse::getId).toList());
        return out;
    }

    // ── MODULE QUIZ OPERATIONS ───────────────────────────────────────

    public Map<String, Object> addModuleQuiz(Map<String, Object> args, ToolDispatchContext ctx) {
        return upsertModuleQuiz(args, ctx, TOOL_ADD_MODULE_QUIZ, "Quiz adaugat: ");
    }

    public Map<String, Object> updateModuleQuiz(Map<String, Object> args, ToolDispatchContext ctx) {
        return upsertModuleQuiz(args, ctx, TOOL_UPDATE_MODULE_QUIZ, "Quiz actualizat: ");
    }

    private Map<String, Object> upsertModuleQuiz(Map<String, Object> args,
                                                  ToolDispatchContext ctx,
                                                  String toolName,
                                                  String messagePrefix) {
        String courseId = requireActiveCourse(ctx);
        if (courseId == null) return errorNoActiveCourse();
        courseManagementService.assertOwner(courseId, ctx.userId());

        String moduleId = requireString(args, "moduleId");
        Course course = loadCourseOrThrow(courseId);
        if (findModuleOrNull(course, moduleId) == null) {
            return errorUnknownId("moduleId", moduleId);
        }

        CreateModuleQuizRequest req = new CreateModuleQuizRequest();
        String title = optionalString(args, "title");
        req.setTitle(title != null ? title : "Quiz modul");
        Integer passing = optionalInt(args, "passingScore");
        if (passing != null) req.setPassingScore(passing);
        Integer timeLimit = optionalInt(args, "timeLimit");
        if (timeLimit != null) req.setTimeLimit(timeLimit);
        req.setQuestions(parseQuestions(args.get("questions")));

        ModuleQuizResponse saved = moduleQuizService.createOrUpdateModuleQuiz(courseId, moduleId, req);
        emit(ctx, toolName, messagePrefix + saved.getTitle());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("moduleQuizId", saved.getId());
        return out;
    }

    public Map<String, Object> deleteModuleQuiz(Map<String, Object> args, ToolDispatchContext ctx) {
        String courseId = requireActiveCourse(ctx);
        if (courseId == null) return errorNoActiveCourse();
        courseManagementService.assertOwner(courseId, ctx.userId());

        String moduleId = requireString(args, "moduleId");
        Course course = loadCourseOrThrow(courseId);
        if (findModuleOrNull(course, moduleId) == null) {
            return errorUnknownId("moduleId", moduleId);
        }

        try {
            moduleQuizService.deleteModuleQuiz(courseId, moduleId);
        } catch (ResourceNotFoundException e) {
            return errorUnknownId("moduleQuizId", "(modul " + moduleId + ")");
        }
        emit(ctx, TOOL_DELETE_MODULE_QUIZ, "Quiz sters pentru modul.");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("moduleId", moduleId);
        return out;
    }

    // ── INTERNALS ────────────────────────────────────────────────────

    /**
     * Returns the active courseId from the route context, or {@code null}
     * if none — the dispatcher then surfaces an actionable error result
     * to the model.
     */
    private static String requireActiveCourse(ToolDispatchContext ctx) {
        RouteContextDTO rc = ctx.routeContext();
        if (rc == null) return null;
        String id = rc.courseId();
        if (id == null || id.isBlank()) return null;
        return id;
    }

    private Course loadCourseOrThrow(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
    }

    private static CourseModule findModuleOrNull(Course course, String moduleId) {
        if (course.getModules() == null) return null;
        return course.getModules().stream()
                .filter(m -> Objects.equals(m.getId(), moduleId))
                .findFirst()
                .orElse(null);
    }

    private static Lecture findLectureOrNull(CourseModule module, String lectureId) {
        if (module.getLectures() == null) return null;
        return module.getLectures().stream()
                .filter(l -> Objects.equals(l.getId(), lectureId))
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Object> errorNoActiveCourse() {
        return Map.of("error", ERR_NO_ACTIVE_COURSE);
    }

    private static Map<String, Object> errorUnknownId(String field, String value) {
        return Map.of("error", field + " necunoscut: " + value);
    }

    @SuppressWarnings("unchecked")
    private static List<ModuleQuizQuestionRequest> parseQuestions(Object raw) {
        List<ModuleQuizQuestionRequest> out = new ArrayList<>();
        if (!(raw instanceof List<?> list)) return out;
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) continue;
            ModuleQuizQuestionRequest q = new ModuleQuizQuestionRequest();
            Object text = m.get("text");
            if (text instanceof String s && !s.isBlank()) q.setText(s);
            else continue;
            Object type = m.get("type");
            q.setType(type instanceof String s ? s : "multiple_choice");
            Object opts = m.get("options");
            if (opts instanceof List<?> ol) {
                List<String> stringOpts = new ArrayList<>();
                for (Object oo : ol) if (oo instanceof String ss) stringOpts.add(ss);
                q.setOptions(stringOpts);
            }
            Object idx = m.get("correctIdx");
            if (idx instanceof Number n) q.setCorrectIdx(n.intValue());
            Object exp = m.get("explanation");
            if (exp instanceof String s) q.setExplanation(s);
            out.add(q);
        }
        return out;
    }

    private static void emit(ToolDispatchContext ctx, String toolName, String message) {
        try {
            ctx.onProgress().accept(new ToolProgressEvent(toolName, 1, 1, message));
        } catch (Exception ignored) {
            // best-effort — progress channel is decorative
        }
    }

    // ── arg helpers — mirror ChatToolHandler's tolerant style ────────

    private static String requireString(Map<String, Object> args, String key) {
        if (args == null) {
            throw new IllegalArgumentException("Missing tool arg: " + key);
        }
        Object v = args.get(key);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Tool arg '" + key + "' must be a non-empty string");
        }
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }

    private static String optionalString(Map<String, Object> args, String key) {
        if (args == null) return null;
        Object v = args.get(key);
        if (v instanceof String s && !s.isBlank()) {
            return s.length() > 4000 ? s.substring(0, 4000) : s;
        }
        return null;
    }

    private static Integer optionalInt(Map<String, Object> args, String key) {
        if (args == null) return null;
        Object v = args.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException nfe) { return null; }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> requireStringList(Map<String, Object> args, String key) {
        if (args == null) {
            throw new IllegalArgumentException("Missing tool arg: " + key);
        }
        Object v = args.get(key);
        if (!(v instanceof List<?> raw) || raw.isEmpty()) {
            throw new IllegalArgumentException("Tool arg '" + key + "' must be a non-empty list");
        }
        List<String> out = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof String s && !s.isBlank()) out.add(s);
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("Tool arg '" + key + "' had no string entries");
        }
        return out;
    }
}
