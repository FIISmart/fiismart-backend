package ro.fiismart.chat.service;

import org.springframework.stereotype.Service;
import ro.fiismart.chat.dto.request.RouteContextDTO;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.Lecture;
import ro.fiismart.common.model.ModuleQuiz;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.ModuleQuizRepository;
import ro.fiismart.common.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builds the system prompt that gets prepended to every chat turn.
 *
 * <p>Goal: give the model just enough situational awareness ("you're on
 * page X, the user is a professor, they're enrolled in Y") to answer
 * concretely, without leaking a vector-DB-sized chunk of platform data
 * into every request. Per the plan, no RAG yet — this is intentional.
 *
 * <p>All lookups are best-effort: if the user, course, or lecture can't
 * be resolved we degrade to a less-specific prompt rather than failing
 * the chat turn. Failing to find a course is not a chat error.
 */
@Service
public class ChatContextBuilder {

    /** Cap the "enrolled courses" line so we don't blow the token budget. */
    private static final int MAX_LISTED_COURSES = 10;

    /** Hard cap on the active-course tree section. We stop emitting
     *  lectures once this many have been printed (across all modules)
     *  and append "..." so the model still sees that more exist. */
    private static final int MAX_DUMPED_LECTURES = 50;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleQuizRepository moduleQuizRepository;

    public ChatContextBuilder(UserRepository userRepository,
                              CourseRepository courseRepository,
                              EnrollmentRepository enrollmentRepository,
                              ModuleQuizRepository moduleQuizRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleQuizRepository = moduleQuizRepository;
    }

    /**
     * Constructs the Romanian system prompt described in the
     * implementation plan. {@code userId} is the Cognito {@code sub}.
     */
    public String buildSystemPrompt(String userId, RouteContextDTO routeContext) {
        Optional<User> userOpt = userRepository.findByCognitoSub(userId);
        String role = userOpt.map(User::getRole).orElse("utilizator");
        String email = userOpt.map(User::getEmail).orElse("");

        String routeDescription = describeRoute(routeContext);

        Optional<Course> activeCourse = resolveActiveCourse(routeContext);
        String courseTitle = activeCourse.map(Course::getTitle).orElse("(niciunul)");

        String lectureLine = resolveLectureLine(activeCourse, routeContext);

        String enrolledCoursesLine = userOpt
                .map(u -> enrolledCoursesFor(u.getId()))
                .orElse("(niciunul)");

        // The prompt is deliberately diacritic-light to keep tokenization
        // consistent across model versions (Gemini handles diacritics but
        // older internal logs/cli tooling sometimes garbles them).
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Esti FIISmart Assistant, un asistent educational pentru platforma FIISmart (in romana).\n");
        sb.append("Utilizator: ").append(role);
        if (email != null && !email.isBlank()) {
            sb.append(" ").append(email);
        }
        sb.append('\n');
        sb.append("Pe pagina: ").append(routeDescription).append('\n');
        sb.append("Curs activ: ").append(courseTitle).append('\n');
        sb.append("Lectia curenta: ").append(lectureLine).append('\n');
        sb.append("Cursurile inrolate: ").append(enrolledCoursesLine).append('\n');
        sb.append("Raspunde concis, in romana. Daca utilizatorul cere sa creezi un quiz sau un curs nou, ");
        sb.append("foloseste tool-urile disponibile (buildFullCourse pentru un curs complet, ");
        sb.append("createQuizDraft pentru un quiz preview).\n\n");

        // Scope restriction — the chatbot is a course-authoring assistant,
        // not a general-purpose chatbot. Politely decline off-topic requests
        // (recipes, weather, sports, personal advice, etc.) and steer the
        // user back to the platform. Without this, users observed Gemini
        // happily answering "da-mi o reteta de clatite".
        sb.append("DOMENIU PERMIS — IMPORTANT:\n");
        sb.append("Esti STRICT un asistent pentru platforma educationala FIISmart. ");
        sb.append("Raspunzi DOAR la intrebari despre: cursuri, module, lectii, quiz-uri, ");
        sb.append("subiecte didactice/academice care pot deveni continut de curs, ");
        sb.append("si despre cum sa folosesti platforma FIISmart.\n");
        sb.append("Daca utilizatorul cere altceva (retete de gatit, vremea, sport, ");
        sb.append("sfaturi personale, glume, chat general, programare neducationala etc.), ");
        sb.append("refuza politicos intr-o singura propozitie scurta si propune o actiune utila ");
        sb.append("pe platforma (ex: 'Pot sa te ajut sa creezi un curs sau un quiz — ce te-ar interesa?'). ");
        sb.append("NU oferi continutul cerut chiar daca utilizatorul insista. ");
        sb.append("Exceptia: daca subiectul off-topic poate deveni un curs didactic ");
        sb.append("(ex: 'creeaza un curs despre gatit'), poti folosi tool-urile pentru a-l construi.\n");

        // Modify-tool gating clarification — when there's no active course,
        // the model used to apologize and tell the user to paste content
        // manually; instead it should ask them to navigate to a course.
        sb.append("\nMODIFICARE CURS — IMPORTANT:\n");
        sb.append("Tool-urile de modificare (addModule, addLecture, updateLecture, etc.) ");
        sb.append("functioneaza DOAR cand utilizatorul este pe pagina unui curs ");
        sb.append("(URL /professor/courses/<id>) iar STAREA CURSULUI ACTIV apare mai jos. ");
        sb.append("Daca nu apare niciun curs activ, NU incerca sa folosesti tool-urile de modificare ");
        sb.append("(vor esua); spune-i utilizatorului sa navigheze la cursul pe care vrea sa-l modifice, ");
        sb.append("apoi sa-ti ceara din nou. Nu oferi continut text ca substitut.");

        // Active course tree dump for modify tools — only when the
        // caller is on a course page AND owns the course. We deliberately
        // gate by ownership: leaking a non-owned course's id tree into
        // the system prompt would let the model attempt edits the BE
        // would then 403 anyway.
        String ownerSub = userOpt.map(User::getCognitoSub).orElse(userId);
        activeCourse
                .filter(c -> ownerSub != null && ownerSub.equals(c.getTeacherId()))
                .ifPresent(course -> appendCourseTree(sb, course));
        return sb.toString();
    }

    /**
     * Renders the "STAREA CURSULUI ACTIV" section: a flat listing of
     * modules, lectures, and any module quiz, each annotated with its
     * id in square brackets. The model is instructed (via the modify-
     * tool descriptions) to copy those bracketed ids verbatim into
     * tool args so we never have to disambiguate at dispatch time.
     */
    private void appendCourseTree(StringBuilder sb, Course course) {
        sb.append("\n\n=== STAREA CURSULUI ACTIV ===\n");
        sb.append("ID curs: ").append(course.getId()).append('\n');
        sb.append("Titlu: ").append(course.getTitle()).append('\n');
        sb.append("Status: ").append(course.getStatus() == null ? "draft" : course.getStatus()).append('\n');

        // Pre-index module quizzes by moduleId so we don't issue one
        // findByModuleId per loop iteration.
        Map<String, ModuleQuiz> quizByModule = new HashMap<>();
        if (course.getId() != null) {
            List<ModuleQuiz> all = moduleQuizRepository.findAllByCourseId(course.getId());
            for (ModuleQuiz q : all) {
                if ("module".equals(q.getQuizScope()) && q.getModuleId() != null) {
                    quizByModule.put(q.getModuleId(), q);
                }
            }
        }

        List<CourseModule> modules = course.getModules();
        if (modules == null || modules.isEmpty()) {
            sb.append("(niciun modul)\n");
            return;
        }
        sb.append("Module:\n");
        int dumpedLectures = 0;
        boolean truncated = false;

        moduleLoop:
        for (CourseModule m : modules) {
            sb.append("  - [").append(safeId(m.getId())).append("] \"")
                    .append(safeText(m.getTitle())).append("\" (ordine ")
                    .append(m.getOrder()).append(")\n");

            List<Lecture> lectures = m.getLectures();
            if (lectures != null && !lectures.isEmpty()) {
                sb.append("      Lectii:\n");
                for (Lecture l : lectures) {
                    if (dumpedLectures >= MAX_DUMPED_LECTURES) {
                        sb.append("        - ... (mai multe lectii nemenționate)\n");
                        truncated = true;
                        break moduleLoop;
                    }
                    sb.append("        - [").append(safeId(l.getId())).append("] \"")
                            .append(safeText(l.getTitle())).append("\" (durata ")
                            .append(l.getDurationSecs()).append("s)\n");
                    dumpedLectures++;
                }
            }
            ModuleQuiz mq = quizByModule.get(m.getId());
            if (mq != null) {
                int qCount = mq.getQuestions() != null ? mq.getQuestions().size() : 0;
                sb.append("      Quiz: [").append(safeId(mq.getId())).append("] \"")
                        .append(safeText(mq.getTitle())).append("\" — ")
                        .append(qCount).append(" intrebari\n");
            }
        }
        if (truncated) {
            sb.append("(...)\n");
        }
        sb.append("Foloseste exact aceste ID-uri cand chemi un tool de modificare. ")
                .append("Nu inventa ID-uri.\n");
    }

    private static String safeId(String id) {
        return id == null ? "?" : id;
    }

    private static String safeText(String t) {
        if (t == null || t.isBlank()) return "(fara titlu)";
        // Quotes are escaped because we wrap titles in literal " — without
        // this a malicious title could close the quoted block early and
        // confuse the model's id parsing.
        String escaped = t.replace("\"", "\\\"");
        return escaped.length() > 120 ? escaped.substring(0, 120) + "..." : escaped;
    }

    private static String describeRoute(RouteContextDTO ctx) {
        if (ctx == null || ctx.route() == null || ctx.route().isBlank()) {
            return "necunoscut";
        }
        // Free-form route string from FE; keep as-is, just trim length so
        // a deliberately-large value can't pad the prompt out of bounds.
        String route = ctx.route();
        return route.length() > 80 ? route.substring(0, 80) : route;
    }

    private Optional<Course> resolveActiveCourse(RouteContextDTO ctx) {
        if (ctx == null || ctx.courseId() == null || ctx.courseId().isBlank()) {
            return Optional.empty();
        }
        return courseRepository.findById(ctx.courseId());
    }

    /**
     * Tries to surface the title of the active lecture. The lecture id
     * lives only as a nested field inside the course's modules, so we
     * walk the in-document list — cheap because modules/lectures per
     * course is small (single-digit usually).
     */
    private String resolveLectureLine(Optional<Course> activeCourse, RouteContextDTO ctx) {
        if (activeCourse.isEmpty() || ctx == null || ctx.lectureId() == null || ctx.lectureId().isBlank()) {
            return "(niciuna)";
        }
        Course course = activeCourse.get();
        if (course.getModules() == null) {
            return "(niciuna)";
        }
        for (CourseModule module : course.getModules()) {
            if (module.getLectures() == null) {
                continue;
            }
            for (Lecture lecture : module.getLectures()) {
                if (ctx.lectureId().equals(lecture.getId())) {
                    return lecture.getTitle() == null ? "(fara titlu)" : lecture.getTitle();
                }
            }
        }
        return "(niciuna)";
    }

    /**
     * Joins the user's enrolled course titles into a comma-separated
     * string, trimmed to the first {@link #MAX_LISTED_COURSES} entries
     * to keep the prompt bounded. Best-effort: missing course documents
     * are skipped silently.
     */
    private String enrolledCoursesFor(String userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(userId);
        if (enrollments == null || enrollments.isEmpty()) {
            return "(niciunul)";
        }
        List<String> ids = enrollments.stream()
                .map(Enrollment::getCourseId)
                .filter(id -> id != null && !id.isBlank())
                .limit(MAX_LISTED_COURSES)
                .toList();
        if (ids.isEmpty()) {
            return "(niciunul)";
        }
        Iterable<Course> courses = courseRepository.findAllById(ids);
        String joined = java.util.stream.StreamSupport.stream(courses.spliterator(), false)
                .map(Course::getTitle)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining(", "));
        if (joined.isEmpty()) {
            return "(niciunul)";
        }
        if (enrollments.size() > MAX_LISTED_COURSES) {
            joined = joined + ", ... (+" + (enrollments.size() - MAX_LISTED_COURSES) + " mai multe)";
        }
        return joined;
    }
}
