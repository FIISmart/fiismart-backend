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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "fiismart.seed.demo-data", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ModuleQuizRepository moduleQuizRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final TutorRequestRepository tutorRequestRepository;
    private final MentorConversationRepository mentorConversationRepository;
    private final CommentRepository commentRepository;
    private final Environment environment;

    @Value("${spring.data.mongodb.uri:}")
    private String mongoUri;

    public DevDataSeeder(UserRepository userRepository,
                         CourseRepository courseRepository,
                         ModuleQuizRepository moduleQuizRepository,
                         EnrollmentRepository enrollmentRepository,
                         QuizAttemptRepository quizAttemptRepository,
                         TutorRequestRepository tutorRequestRepository,
                         MentorConversationRepository mentorConversationRepository,
                         CommentRepository commentRepository,
                         Environment environment) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.moduleQuizRepository = moduleQuizRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.tutorRequestRepository = tutorRequestRepository;
        this.mentorConversationRepository = mentorConversationRepository;
        this.commentRepository = commentRepository;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean isDevOrLocal = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "dev".equalsIgnoreCase(p) || "local".equalsIgnoreCase(p));
        if (!isDevOrLocal) {
            log.warn("[Seed] Skipping demo seed - no 'dev' or 'local' profile active. Active profiles: {}",
                    Arrays.toString(environment.getActiveProfiles()));
            return;
        }

        if (mongoUri != null && mongoUri.contains("mongodb.net")) {
            log.error("[Seed] REFUSING to seed: spring.data.mongodb.uri points at Mongo Atlas.");
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
                upsertStudent("student.andrei.popa@fiismart.local", "Andrei Popa", "Facultatea de Informatica", "Informatica", 1, "STUDENT", List.of("Java", "Algoritmica")),
                upsertStudent("student.maria.iancu@fiismart.local", "Maria Iancu", "Facultatea de Informatica", "Informatica", 2, "STUDENT", List.of("React", "UI/UX")),
                upsertStudent("student.tudor.neagu@fiismart.local", "Tudor Neagu", "Automatica si Calculatoare", "Calculatoare", 3, "STUDENT", List.of("Backend", "Baze de date")),
                upsertStudent("student.elena.rusu@fiismart.local", "Elena Rusu", "Colegiul National", "Matematica-Informatica", 12, "ELEV", List.of("Admitere", "Programare")),
                upsertStudent("student.alex.barbu@fiismart.local", "Alex Barbu", "Facultatea de Informatica", "Informatica", 1, "STUDENT", List.of("Python", "AI")),
                upsertStudent("student.bianca.matei@fiismart.local", "Bianca Matei", "Facultatea de Matematica", "Informatica aplicata", 2, "STUDENT", List.of("SQL", "Modelare date")),
                upsertStudent("student.cristian.dobre@fiismart.local", "Cristian Dobre", "Automatica si Calculatoare", "Ingineria sistemelor", 3, "STUDENT", List.of("DevOps", "Git")),
                upsertStudent("student.daria.ene@fiismart.local", "Daria Ene", "Facultatea de Informatica", "Informatica", 2, "STUDENT", List.of("Frontend", "TypeScript")),
                upsertStudent("student.vlad.munteanu@fiismart.local", "Vlad Munteanu", "Colegiul National", "Matematica-Informatica", 11, "ELEV", List.of("Algoritmica", "C++")),
                upsertStudent("student.irina.dragomir@fiismart.local", "Irina Dragomir", "Facultatea de Informatica", "Informatica", 3, "STUDENT", List.of("Machine Learning", "Python"))
        );
        upsertAdmin("admin.demo@fiismart.local", "Admin Demo");

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
        upsertComments(students, courses);

        log.info("[Seed] Demo data ready. Users={}, Courses={}, ModuleQuiz={}, Enrollments={}, QuizAttempt={}, Comments={}, TutorRequests={}, MentorConversations={}",
                userRepository.count(),
                courseRepository.count(),
                moduleQuizRepository.count(),
                enrollmentRepository.count(),
                quizAttemptRepository.count(),
                commentRepository.count(),
                tutorRequestRepository.count(),
                mentorConversationRepository.count());
    }

    private User upsertTutor(String email, String displayName, String headline, String bio, List<String> subjects,
                             double rating, int reviewCount, int experienceYears, String availability) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setRole("professor");
        user.setHeadline(headline);
        user.setBio(bio);
        user.setFaculty("Universitatea Alexandru Ioan Cuza");
        user.setDepartment(departmentFor(subjects));
        user.setAcademicTitle("Mentor FII Smart");
        user.setEducationLevel("PROFESOR");
        user.setTutorProfileEnabled(true);
        user.setInterests(new ArrayList<>(subjects));
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

    private User upsertStudent(String email, String displayName, String faculty, String specialization, int studyYear, String educationLevel, List<String> interests) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setRole("student");
        user.setFaculty(faculty);
        user.setSpecialization(specialization);
        user.setStudyYear(studyYear);
        user.setEducationLevel(educationLevel);
        user.setBio(displayName + " foloseste FII Smart pentru a invata prin cursuri aplicate, quiz-uri si mentorat.");
        user.setInterests(new ArrayList<>(interests));
        user.setSubjects(new ArrayList<>(interests));
        user.setBanned(false);
        if (user.getCreatedAt() == null) user.setCreatedAt(new Date());
        return userRepository.save(user);
    }

    private User upsertAdmin(String email, String displayName) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setRole("admin");
        user.setBio("Cont demo pentru administrarea platformei FII Smart.");
        user.setEducationLevel("ADMIN");
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
        return CourseModule.builder()
                .id(id)
                .title(title)
                .description("Lectii scurte si aplicate")
                .order(order)
                .lectures(lectures)
                .build();
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
                .questions(List.of(
                        mc("Care este primul pas recomandat?", List.of("Citirea obiectivelor", "Sarirea peste exemple", "Memorarea fara practica"), 0),
                        written("Scrie un concept important din lectie.", course.getTags().get(0))
                ))
                .build());

        moduleQuizRepository.save(ModuleQuiz.builder()
                .courseId(course.getId())
                .moduleId(firstModule.getId())
                .quizScope("module")
                .title("Verificare modul: " + firstModule.getTitle())
                .passingScore(70)
                .timeLimit(15)
                .questions(List.of(
                        mc("Ce ajuta cel mai mult la invatare?", List.of("Exercitiile practice", "Ignorarea feedbackului", "Copierea solutiilor"), 0)
                ))
                .build());

        moduleQuizRepository.save(ModuleQuiz.builder()
                .courseId(course.getId())
                .quizScope("course_final")
                .title("Quiz final: " + course.getTitle())
                .passingScore(75)
                .timeLimit(25)
                .questions(List.of(
                        mc("Ce reprezinta un curs finalizat?", List.of("Lectii parcurse si concepte intelese", "Doar deschiderea paginii", "Un singur click"), 0),
                        written("Mentioneaza o abilitate exersata in curs.", course.getTags().get(0))
                ))
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
        TutorRequest accepted = upsertTutorRequest(students.get(1), tutors.get(1), "Vreau sa inteleg mai bine cum leg un controller de service.", "accepted");
        TutorRequest resolved = upsertTutorRequest(students.get(2), tutors.get(2), "Ma pregatesc pentru examenul de baze de date.", "resolved");
        upsertTutorRequest(students.get(3), tutors.get(3), "As vrea sa exersez probleme de complexitate.", "declined");
        upsertTutorRequest(students.get(4), tutors.get(4), "Vreau sa pornesc un proiect mic de Python si AI.", "pending");
        upsertTutorRequest(students.get(5), tutors.get(2), "Am probleme cu normalizarea tabelelor.", "accepted");
        upsertTutorRequest(students.get(6), tutors.get(5), "Vreau sa inteleg mai bine conflictele Git.", "resolved");
        upsertTutorRequest(students.get(7), tutors.get(6), "As vrea feedback pe un layout de dashboard.", "pending");
        upsertTutorRequest(students.get(8), tutors.get(3), "Ma pregatesc pentru concursuri de algoritmica.", "accepted");
        upsertTutorRequest(students.get(9), tutors.get(4), "Nu inteleg diferenta dintre clasificare si regresie.", "declined");
        upsertMentorConversation(accepted, "Salut, am acceptat cererea. Putem incepe cu structura controller-service.");
        upsertMentorConversation(resolved, "Am revazut schema relationala. Mai exerseaza JOIN-urile si cheile externe.");
        tutorRequestRepository.findByStudentId(students.get(5).getId()).stream()
                .filter(request -> tutors.get(2).getId().equals(request.getTutorId()))
                .findFirst()
                .ifPresent(request -> upsertMentorConversation(request, "Sigur, incepem cu formele normale si exemple simple."));
        tutorRequestRepository.findByStudentId(students.get(8).getId()).stream()
                .filter(request -> tutors.get(3).getId().equals(request.getTutorId()))
                .findFirst()
                .ifPresent(request -> upsertMentorConversation(request, "Putem lucra pe probleme cu sortari si complexitate."));
    }

    private TutorRequest upsertTutorRequest(User student, User tutor, String message, String status) {
        Optional<TutorRequest> existing = tutorRequestRepository.findByStudentId(student.getId()).stream()
                .filter(request -> tutor.getId().equals(request.getTutorId()) && status.equalsIgnoreCase(request.getStatus()))
                .findFirst();
        if (existing.isPresent()) return existing.get();
        return tutorRequestRepository.save(TutorRequest.builder()
                .studentId(student.getId())
                .tutorId(tutor.getId())
                .message(message)
                .status(status)
                .createdAt(new Date())
                .build());
    }

    private void upsertMentorConversation(TutorRequest request, String tutorReply) {
        if (request == null) return;
        MentorConversation conversation = mentorConversationRepository.findByRequestId(request.getId()).orElse(null);
        if (conversation == null) {
            User student = userRepository.findById(request.getStudentId()).orElse(null);
            User tutor = userRepository.findById(request.getTutorId()).orElse(null);
            Date now = new Date();
            conversation = mentorConversationRepository.save(MentorConversation.builder()
                    .requestId(request.getId())
                    .studentId(request.getStudentId())
                    .tutorId(request.getTutorId())
                    .messages(new ArrayList<>(List.of(
                            MentorMessage.builder()
                                    .id(UUID.randomUUID().toString())
                                    .senderId(request.getStudentId())
                                    .senderName(student != null ? student.getDisplayName() : "Student FII Smart")
                                    .senderRole("student")
                                    .text(request.getMessage())
                                    .createdAt(Date.from(Instant.now().minusSeconds(3600)))
                                    .build(),
                            MentorMessage.builder()
                                    .id(UUID.randomUUID().toString())
                                    .senderId(request.getTutorId())
                                    .senderName(tutor != null ? tutor.getDisplayName() : "Profesor FII Smart")
                                    .senderRole("professor")
                                    .text(tutorReply)
                                    .createdAt(Date.from(Instant.now().minusSeconds(2400)))
                                    .build()
                    )))
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        if (request.getConversationId() == null || !request.getConversationId().equals(conversation.getId())) {
            request.setConversationId(conversation.getId());
            tutorRequestRepository.save(request);
        }
    }

    private void upsertComments(List<User> students, List<Course> courses) {
        List<String> bodies = List.of(
                "Nu am inteles diferenta dintre class si object.",
                "Puteti explica inca o data JOIN-ul din exemplul 2?",
                "La exercitiul cu branch-uri, de ce apare conflict?",
                "Care este diferenta dintre useState si useEffect?",
                "Cum alegem cheia primara intr-un tabel?",
                "De ce complexitatea O(n log n) este mai buna decat O(n^2)?",
                "Cum pot verifica daca API-ul returneaza datele corecte?",
                "Ce inseamna sa separ logica in service si controller?",
                "Exista un exemplu simplu pentru liste inlantuite?",
                "Cum alegem un model potrivit pentru o problema AI?"
        );

        int index = 0;
        for (Course course : courses) {
            if (course.getModules() == null || course.getModules().isEmpty()) continue;
            CourseModule module = course.getModules().get(0);
            if (module.getLectures() == null || module.getLectures().isEmpty()) continue;
            Lecture lecture = module.getLectures().get(0);
            for (int i = 0; i < 3; i++) {
                User student = students.get((index + i) % students.size());
                String body = bodies.get((index + i) % bodies.size());
                String status = switch ((index + i) % 3) {
                    case 0 -> "OPEN";
                    case 1 -> "ANSWERED";
                    default -> "RESOLVED";
                };
                Comment comment = upsertComment(course, lecture, student, body, status);
                if (!"OPEN".equals(status)) {
                    User teacher = userRepository.findById(course.getTeacherId()).orElse(null);
                    upsertReply(comment, teacher, teacherReplyFor(body));
                }
            }
            index++;
        }
    }

    private Comment upsertComment(Course course, Lecture lecture, User student, String body, String status) {
        Optional<Comment> existing = commentRepository.findByCourseId(course.getId()).stream()
                .filter(comment -> lecture.getId().equals(comment.getLectureId()))
                .filter(comment -> student.getId().equals(comment.getAuthorId()))
                .filter(comment -> body.equals(comment.getBody()))
                .filter(comment -> comment.getParentCommentId() == null)
                .findFirst();
        Comment comment = existing.orElseGet(Comment::new);
        comment.setCourseId(course.getId());
        comment.setLectureId(lecture.getId());
        comment.setAuthorId(student.getId());
        comment.setBody(body);
        comment.setStatus(status);
        comment.setDeleted(false);
        if (comment.getCreatedAt() == null) {
            comment.setCreatedAt(Date.from(Instant.now().minusSeconds(86400L + Math.abs(body.hashCode() % 604800))));
        }
        comment.setUpdatedAt(new Date());
        return commentRepository.save(comment);
    }

    private void upsertReply(Comment parent, User teacher, String body) {
        if (teacher == null) return;
        boolean exists = commentRepository.findRepliesByParentId(parent.getId()).stream()
                .anyMatch(reply -> teacher.getId().equals(reply.getAuthorId()) && body.equals(reply.getBody()));
        if (exists) return;
        commentRepository.save(Comment.builder()
                .courseId(parent.getCourseId())
                .lectureId(parent.getLectureId())
                .authorId(teacher.getId())
                .parentCommentId(parent.getId())
                .body(body)
                .status("ANSWERED")
                .createdAt(Date.from(parent.getCreatedAt().toInstant().plusSeconds(3600)))
                .updatedAt(new Date())
                .deleted(false)
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

    private String departmentFor(List<String> subjects) {
        if (subjects.stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains("design"))) return "Design si interactiune om-calculator";
        if (subjects.stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains("sql"))) return "Sisteme informatice";
        if (subjects.stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains("algorit"))) return "Fundamentele informaticii";
        return "Informatica aplicata";
    }

    private String teacherReplyFor(String body) {
        if (body.contains("JOIN")) return "Revino la exemplul cu doua tabele si urmareste cheia comuna folosita in conditia ON.";
        if (body.contains("useState")) return "useState pastreaza valori in componenta, iar useEffect ruleaza efecte dupa randare.";
        if (body.contains("class")) return "Clasa este sablonul, obiectul este instanta concreta creata din acel sablon.";
        if (body.contains("cheia primara")) return "Alege un camp stabil, unic si nenul; daca nu exista, foloseste un identificator generat.";
        if (body.contains("conflict")) return "Conflictul apare cand doua branch-uri modifica aceeasi zona si Git nu poate decide automat.";
        return "Intrebarea este buna. Uita-te la pasii din lectie si incearca sa refaci exemplul pe o varianta mai mica.";
    }
}
