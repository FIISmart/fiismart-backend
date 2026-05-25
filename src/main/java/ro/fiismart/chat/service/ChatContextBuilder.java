package ro.fiismart.chat.service;

import org.springframework.stereotype.Service;
import ro.fiismart.chat.dto.request.RouteContextDTO;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.Lecture;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.UserRepository;

import java.util.List;
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

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public ChatContextBuilder(UserRepository userRepository,
                              CourseRepository courseRepository,
                              EnrollmentRepository enrollmentRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
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
        StringBuilder sb = new StringBuilder(512);
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
        sb.append("Raspunde concis, in romana. Daca utilizatorul cere sa creezi un quiz sau un curs, ");
        sb.append("foloseste tool-urile disponibile (createQuizDraft, createCourseDraft).");
        return sb.toString();
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
