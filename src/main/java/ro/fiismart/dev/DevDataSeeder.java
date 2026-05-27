package ro.fiismart.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ro.fiismart.common.model.*;
import ro.fiismart.common.repository.*;

import java.time.Instant;
import java.util.*;

@Component
@ConditionalOnProperty(prefix = "fiismart.seed.demo-data", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ModuleQuizRepository moduleQuizRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final TutorRequestRepository tutorRequestRepository;
    private final Environment environment;

    @Value("${spring.data.mongodb.uri:}")
    private String mongoUri;

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    public DevDataSeeder(UserRepository userRepository,
                         CourseRepository courseRepository,
                         ModuleQuizRepository moduleQuizRepository,
                         EnrollmentRepository enrollmentRepository,
                         QuizAttemptRepository quizAttemptRepository,
                         TutorRequestRepository tutorRequestRepository,
                         Environment environment) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.moduleQuizRepository = moduleQuizRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.tutorRequestRepository = tutorRequestRepository;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Allow-list, not deny-list. Bail unless a `dev` or `local` profile
        // is explicitly active. Railway and similar hosts run with no active
        // profile by default — a deny-list (`bail if prod`) is a foot-gun
        // since one env-var typo (`FIISMART_SEED_DEMO_DATA_ENABLED=true`)
        // would then seed staging or prod Mongo.
        boolean isDevOrLocal = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "dev".equalsIgnoreCase(p) || "local".equalsIgnoreCase(p));
        if (!isDevOrLocal) {
            log.warn("[Seed] Skipping demo seed — no 'dev' or 'local' profile active. " +
                    "Active profiles: {}", Arrays.toString(environment.getActiveProfiles()));
            return;
        }

        // Belt-and-suspenders: refuse to seed if pointed at Atlas, which is
        // where our shared staging/prod cluster lives.
        if (mongoUri != null && mongoUri.contains("mongodb.net")) {
            log.error("[Seed] REFUSING to seed: spring.data.mongodb.uri points at Mongo Atlas " +
                    "(host contains 'mongodb.net'). Seeder is for local-only DBs.");
            return;
        }

        List<User> tutors = List.of(
                upsertTutor("ana.popescu@fiismart.local", "Ana Popescu", "Mentor React si Frontend",
                        "Lucreaza cu studenti care vor sa construiasca interfete moderne si usor de folosit.",
                        List.of("React", "JavaScript", "Frontend"), 4.9, 38, 6, "Disponibila saptamana aceasta"),
                upsertTutor("mihai.ionescu@fiismart.local", "Mihai Ionescu", "Java si Spring Boot",
                        "Explica backend, API-uri REST si arhitectura aplicatiilor web pas cu pas.",
                        List.of("Java", "Spring Boot", "Backend"), 4.8, 31, 8, "Locuri limitate"),
                upsertTutor("elena.dumitrescu@fiismart.local", "Elena Dumitrescu", "SQL si baze de date",
                        "Ajuta studentii sa inteleaga modelarea datelor, SQL si optimizarea interogarilor.",
                        List.of("SQL", "Baze de Date", "Modelare"), 4.7, 24, 7, "Disponibila"),
                upsertTutor("radu.marinescu@fiismart.local", "Radu Marinescu", "Algoritmica si structuri de date",
                        "Pregatire pentru examene si interviuri tehnice, cu probleme explicate clar.",
                        List.of("Algoritmica", "Structuri de Date", "C++"), 4.9, 45, 9, "Disponibil seara"),
                upsertTutor("ioana.pavel@fiismart.local", "Ioana Pavel", "Python si AI Basics",
                        "Introduce conceptele de Python, date si machine learning pentru incepatori.",
                        List.of("Python", "AI Basics", "Machine Learning"), 4.8, 29, 5, "Disponibila online"),
                upsertTutor("andrei.stan@fiismart.local", "Andrei Stan", "Git si DevOps Basics",
                        "Te ajuta sa colaborezi mai bine in proiecte software folosind Git si workflow-uri curate.",
                        List.of("Git", "DevOps", "Colaborare"), 4.6, 18, 6, "Disponibil weekend"),
                upsertTutor("maria.neagu@fiismart.local", "Maria Neagu", "UI/UX pentru aplicatii web",
                        "Lucreaza cu studenti interesati de design, prototipare si experienta utilizatorului.",
                        List.of("UI/UX", "Design", "Figma"), 4.8, 22, 7, "Disponibila")
        );

        List<User> students = List.of(
                upsertStudent("student.alex@fiismart.local", "Alex Barbu"),
                upsertStudent("student.bianca@fiismart.local", "Bianca Matei"),
                upsertStudent("student.cristian@fiismart.local", "Cristian Dobre"),
                upsertStudent("student.daria@fiismart.local", "Daria Ene")
        );

        List<Course> courses = List.of(
                upsertCourse("Introducere in Programare Java", tutors.get(1), List.of("Java", "Programare", "OOP"), "Invata Java de la sintaxa de baza pana la clase si obiecte."),
                upsertCourse("Web Development cu React", tutors.get(0), List.of("React", "TypeScript", "Frontend"), "Construieste aplicatii web moderne cu componente, hooks si state management."),
                upsertCourse("Baze de Date si SQL", tutors.get(2), List.of("SQL", "Baze de Date", "Modelare"), "Invata SELECT, JOIN, chei primare si modelare relationala."),
                upsertCourse("Algoritmica si Structuri de Date", tutors.get(3), List.of("Algoritmica", "Structuri de Date", "Interviuri"), "Exerseaza complexitate, sortari, liste, arbori si grafuri."),
                upsertCourse("Spring Boot pentru aplicatii web", tutors.get(1), List.of("Spring Boot", "Java", "REST"), "Construieste API-uri robuste cu Spring Boot si MongoDB."),
                upsertCourse("Python pentru incepatori", tutors.get(4), List.of("Python", "Programare", "Automatizare"), "Invata Python prin exemple practice si exercitii scurte."),
                upsertCourse("Introducere in Inteligenta Artificiala", tutors.get(4), List.of("AI", "Machine Learning", "Python"), "Intelege datele, modelele si predictiile prin exemple usor de urmat."),
                upsertCourse("Git si colaborare in echipe software", tutors.get(5), List.of("Git", "DevOps", "Colaborare"), "Invata branch-uri, pull requests si workflow-uri de echipa.")
        );

        upsertCourse("Prototipare UI in Figma", tutors.get(6), List.of("Figma", "Design", "UI/UX"), "draft");
        upsertCourse("Testare automata pentru web", tutors.get(5), List.of("Testing", "QA", "Automation"), "draft");

        for (Course course : courses) {
            upsertQuizzes(course);
        }

        upsertEnrollmentsAndAttempts(students, courses);
        upsertTutorRequests(students, tutors);
    }

    private User upsertTutor(String email, String displayName, String headline, String bio, List<String> subjects,
                             double rating, int reviewCount, int experienceYears, String availability) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setRole("professor");
        user.setHeadline(headline);
        user.setBio(bio);
        user.setSubjects(new ArrayList<>(subjects));
        user.setTutorRating(rating);
        user.setTutorReviewCount(reviewCount);
        user.setExperienceYears(experienceYears);
        user.setAvailability(availability);
        user.setPriceLabel("Gratuit");
        user.setBanned(false);
        if (user.getCreatedAt() == null) user.setCreatedAt(new Date());
        return userRepository.save(user);
    }

    private User upsertStudent(String email, String displayName) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setRole("student");
        user.setBanned(false);
        if (user.getCreatedAt() == null) user.setCreatedAt(new Date());
        return userRepository.save(user);
    }

    private Course upsertCourse(String title, User teacher, List<String> tags, String description) {
        return upsertCourse(title, teacher, tags, description, "published");
    }

    private Course upsertCourse(String title, User teacher, List<String> tags, String description, String status) {
        Optional<Course> existing = courseRepository.findAll().stream()
                .filter(course -> title.equalsIgnoreCase(safe(course.getTitle())))
                .findFirst();
        if (existing.isPresent()) return existing.get();

        String moduleOneId = UUID.randomUUID().toString();
        String moduleTwoId = UUID.randomUUID().toString();
        CourseModule moduleOne = module(moduleOneId, "Fundamente", 1, List.of(
                lecture(moduleOneId, "Ce vei invata in acest curs", "markdown", introContent(title), null, 1, 360),
                lecture(moduleOneId, "Lectie video introductiva", "video", null, "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 2, 240)
        ));
        CourseModule moduleTwo = module(moduleTwoId, "Aplicatii practice", 2, List.of(
                lecture(moduleTwoId, "Exercitiu ghidat", "markdown", exerciseContent(tags.get(0)), null, 1, 480),
                lecture(moduleTwoId, "Recapitulare si pasi urmatori", "markdown", recapContent(title), null, 2, 300)
        ));

        Course course = Course.builder()
                .title(title)
                .description(description)
                .teacherId(teacher.getId())
                .status(status)
                .tags(new ArrayList<>(tags))
                .thumbnailUrl(null)
                .language("ro")
                .enrollmentCount("published".equals(status) ? 12 : 0)
                .avgRating(teacher.getTutorRating() != null ? teacher.getTutorRating() : 4.7)
                .modules(List.of(moduleOne, moduleTwo))
                .hidden(false)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        Course saved = courseRepository.save(course);
        if (teacher.getOwnedCourses() == null) teacher.setOwnedCourses(new ArrayList<>());
        if (!teacher.getOwnedCourses().contains(saved.getId())) {
            teacher.getOwnedCourses().add(saved.getId());
            userRepository.save(teacher);
        }
        return saved;
    }

    private CourseModule module(String id, String title, int order, List<Lecture> lectures) {
        return CourseModule.builder().id(id).title(title).description("Lectii scurte si aplicate").order(order).lectures(lectures).build();
    }

    private Lecture lecture(String moduleId, String title, String type, String content, String videoUrl, int order, int durationSecs) {
        return Lecture.builder()
                .id(UUID.randomUUID().toString())
                .moduleId(moduleId)
                .title(title)
                .type(type)
                .content(content)
                .videoUrl(videoUrl)
                .order(order)
                .durationSecs(durationSecs)
                .publishedAt(new Date())
                .build();
    }

    private void upsertQuizzes(Course course) {
        if (!moduleQuizRepository.findAllByCourseId(course.getId()).isEmpty()) return;
        CourseModule firstModule = course.getModules().get(0);
        Lecture firstLecture = firstModule.getLectures().get(0);

        moduleQuizRepository.save(ModuleQuiz.builder()
                .courseId(course.getId())
                .moduleId(firstModule.getId())
                .lectureId(firstLecture.getId())
                .quizScope("lecture")
                .title("Quiz rapid: " + firstLecture.getTitle())
                .passingScore(70)
                .timeLimit(10)
                .questions(List.of(mc("Care este primul pas recomandat?", List.of("Citirea obiectivelor", "Sarirea peste exemple", "Memorarea fara practica"), 0), written("Scrie un concept important din lectie.", course.getTags().get(0))))
                .build());

        moduleQuizRepository.save(ModuleQuiz.builder()
                .courseId(course.getId())
                .moduleId(firstModule.getId())
                .quizScope("module")
                .title("Verificare modul: " + firstModule.getTitle())
                .passingScore(70)
                .timeLimit(15)
                .questions(List.of(mc("Ce ajuta cel mai mult la invatare?", List.of("Exercitiile practice", "Ignorarea feedbackului", "Copierea solutiilor"), 0)))
                .build());

        moduleQuizRepository.save(ModuleQuiz.builder()
                .courseId(course.getId())
                .quizScope("course_final")
                .title("Quiz final: " + course.getTitle())
                .passingScore(75)
                .timeLimit(25)
                .questions(List.of(mc("Ce reprezinta un curs finalizat?", List.of("Lectii parcurse si concepte intelese", "Doar deschiderea paginii", "Un singur click"), 0), written("Mentioneaza o abilitate exersata in curs.", course.getTags().get(0))))
                .build());
    }

    private ModuleQuizQuestion mc(String text, List<String> options, int correctIdx) {
        return ModuleQuizQuestion.builder()
                .id(UUID.randomUUID().toString())
                .text(text)
                .type("multiple_choice")
                .points(1)
                .options(options)
                .correctIdx(correctIdx)
                .explanation("Raspunsul corect urmareste aplicarea practica a conceptului.")
                .build();
    }

    private ModuleQuizQuestion written(String text, String correctText) {
        return ModuleQuizQuestion.builder()
                .id(UUID.randomUUID().toString())
                .text(text)
                .type("written")
                .points(1)
                .options(List.of())
                .correctText(correctText)
                .explanation("Un raspuns scurt este suficient daca surprinde conceptul principal.")
                .build();
    }

    private void upsertEnrollmentsAndAttempts(List<User> students, List<Course> courses) {
        for (int i = 0; i < students.size(); i++) {
            User student = students.get(i);
            final int studentIndex = i;
            for (int c = 0; c < Math.min(courses.size(), 4); c++) {
                Course course = courses.get((i + c) % courses.size());
                if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
                    int completedLectures = Math.min(course.getModules().size() + i, 4);
                    List<LectureProgressEntry> progress = progressEntries(course, completedLectures);
                    enrollmentRepository.save(Enrollment.builder()
                            .studentId(student.getId())
                            .courseId(course.getId())
                            .enrolledAt(new Date())
                            .status(completedLectures >= 4 ? "completed" : "active")
                            .completedAt(completedLectures >= 4 ? new Date() : null)
                            .lectureProgress(progress)
                            .lastAccessedAt(new Date())
                            .overallProgress(Math.min(100, completedLectures * 25))
                            .build());
                }

                moduleQuizRepository.findAllByCourseId(course.getId()).stream().limit(2).forEach(quiz -> {
                    if (quizAttemptRepository.findByStudentIdAndQuizId(student.getId(), quiz.getId()).isEmpty()) {
                        int score = 65 + (studentIndex * 7) % 30;
                        quizAttemptRepository.save(QuizAttempt.builder()
                                .quizId(quiz.getId())
                                .courseId(course.getId())
                                .studentId(student.getId())
                                .attemptedAt(new Date())
                                .score(score)
                                .passed(score >= quiz.getPassingScore())
                                .timeTakenSecs(300 + studentIndex * 40)
                                .startedAt(Instant.now().minusSeconds(600))
                                .status("SUBMITTED")
                                .answers(List.of(Answer.builder().questionId("seeded").selectedIdx(0).correct(score >= 70).build()))
                                .build());
                    }
                });
            }
        }
    }

    private List<LectureProgressEntry> progressEntries(Course course, int completedLectures) {
        List<LectureProgressEntry> entries = new ArrayList<>();
        int index = 0;
        for (CourseModule module : course.getModules()) {
            for (Lecture lecture : module.getLectures()) {
                boolean completed = index < completedLectures;
                entries.add(LectureProgressEntry.builder()
                        .moduleId(module.getId())
                        .lectureId(lecture.getId())
                        .watchedPercent(completed ? 100 : 35)
                        .positionSecs(completed ? lecture.getDurationSecs() : Math.min(120, lecture.getDurationSecs()))
                        .completed(completed)
                        .updatedAt(new Date())
                        .build());
                index++;
            }
        }
        return entries;
    }

    private void upsertTutorRequests(List<User> students, List<User> tutors) {
        upsertTutorRequest(students.get(0), tutors.get(0), "Am nevoie de ajutor la hooks si structurarea componentelor.", "pending");
        upsertTutorRequest(students.get(1), tutors.get(1), "Vreau sa inteleg mai bine cum leg un controller de service.", "accepted");
        upsertTutorRequest(students.get(2), tutors.get(2), "Ma pregatesc pentru examenul de baze de date.", "resolved");
        upsertTutorRequest(students.get(3), tutors.get(3), "As vrea sa exersez probleme de complexitate.", "declined");
    }

    private void upsertTutorRequest(User student, User tutor, String message, String status) {
        if (tutorRequestRepository.existsByStudentIdAndTutorIdAndStatus(student.getId(), tutor.getId(), status)) return;
        tutorRequestRepository.save(TutorRequest.builder()
                .studentId(student.getId())
                .tutorId(tutor.getId())
                .message(message)
                .status(status)
                .createdAt(new Date())
                .build());
    }

    private String introContent(String title) {
        return "## " + title + "\n\nIn aceasta lectie clarificam obiectivele cursului si modul in care vei exersa conceptele importante.";
    }

    private String exerciseContent(String topic) {
        return "## Exercitiu practic\n\nAlege un exemplu simplu din zona " + topic + " si explica fiecare pas inainte sa scrii solutia.";
    }

    private String recapContent(String title) {
        return "## Recapitulare\n\nNoteaza ideile principale din " + title + " si revino la quiz pentru verificare.";
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
