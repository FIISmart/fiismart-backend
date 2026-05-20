package ro.fiismart.quizattempt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ConflictException;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Answer;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.Lecture;
import ro.fiismart.common.model.LectureProgressEntry;
import ro.fiismart.common.model.ModuleQuiz;
import ro.fiismart.common.model.ModuleQuizQuestion;
import ro.fiismart.common.model.QuizAttempt;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.ModuleQuizRepository;
import ro.fiismart.common.repository.QuizAttemptRepository;
import ro.fiismart.quizattempt.dto.QuizAttemptRequest;
import ro.fiismart.quizattempt.dto.QuizAttemptResponse;
import ro.fiismart.quizattempt.dto.StartQuizAttemptResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    /** Wall-clock slack on the deadline so a borderline-late submit still counts. */
    private static final int GRACE_SECONDS = 5;

    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleQuizRepository moduleQuizRepository;
    private final MongoTemplate mongoTemplate;

    // ── New lifecycle flow (start → submit / abandon) ────────────────────────

    /**
     * Begin a fresh attempt for {@code studentId} on {@code quizId}.
     *
     * <p>Idempotent: if an {@code IN_PROGRESS} attempt already exists for this
     * (student, quiz) pair, returns it instead of creating a duplicate. This
     * stops a double-click on the "Start" button from spawning two stopwatches.</p>
     */
    public StartQuizAttemptResponse startAttempt(String studentId, String quizId) {
        ModuleQuiz quiz = moduleQuizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));

        // Idempotency guard — re-use an already-running attempt.
        QuizAttempt existing = quizAttemptRepository
                .findFirstByStudentIdAndQuizIdAndStatus(studentId, quizId, "IN_PROGRESS")
                .orElse(null);
        if (existing != null) {
            return new StartQuizAttemptResponse(
                    existing.getId(),
                    existing.getStartedAt(),
                    // quiz.timeLimit is in MINUTES (see QuizRequest defaulting to 30).
                    quiz.getTimeLimit() * 60,
                    existing.getStatus()
            );
        }

        Instant now = Instant.now();
        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(quizId)
                .courseId(quiz.getCourseId())  // derive from quiz, never trust client
                .studentId(studentId)
                .attemptedAt(Date.from(now))
                .startedAt(now)
                .status("IN_PROGRESS")
                .score(0)
                .passed(false)
                .answers(new ArrayList<>())
                .build();
        QuizAttempt saved = quizAttemptRepository.save(attempt);

        // quiz.timeLimit is MINUTES (see ro.fiismart.quiz.dto.QuizRequest:15 default=30).
        // Convert exactly once, here, so every downstream caller speaks seconds.
        int timeLimitSeconds = quiz.getTimeLimit() * 60;

        return new StartQuizAttemptResponse(
                saved.getId(),
                saved.getStartedAt(),
                timeLimitSeconds,
                saved.getStatus()
        );
    }

    /**
     * Finalize an attempt. Server-grades the answers (clients cannot tell us
     * their score), enforces ownership, status transitions, and the deadline
     * (with a small grace window). Also refreshes the student's overall
     * course progress as a side-effect.
     */
    public QuizAttemptResponse submitAttempt(String studentId, String attemptId, List<Answer> clientAnswers) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("QuizAttempt not found: " + attemptId));

        if (!Objects.equals(attempt.getStudentId(), studentId)) {
            throw new ForbiddenException("Not your attempt");
        }

        // Status transition guard (defense in depth — DB constraint can't express this).
        if ("SUBMITTED".equals(attempt.getStatus())) {
            throw new ConflictException("Already submitted");
        }
        if ("ABANDONED".equals(attempt.getStatus())) {
            throw new ConflictException("Attempt was abandoned");
        }

        ModuleQuiz quiz = moduleQuizRepository.findById(attempt.getQuizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + attempt.getQuizId()));

        // Tolerate legacy attempts that pre-date this feature and have no startedAt.
        Instant now = Instant.now();
        Duration elapsed = attempt.getStartedAt() != null
                ? Duration.between(attempt.getStartedAt(), now)
                : Duration.ZERO;

        int durationSeconds = quiz.getTimeLimit() * 60;  // MINUTES → seconds
        boolean expired = elapsed.toSeconds() > (long) durationSeconds + GRACE_SECONDS;

        // SERVER-SIDE GRADING. Clients never supply score/passed.
        List<Answer> graded = gradeAnswers(quiz, clientAnswers != null ? clientAnswers : new ArrayList<>());
        int score = computeScore(quiz, graded);
        boolean passed = score >= quiz.getPassingScore();

        attempt.setAnswers(graded);
        attempt.setScore(score);
        attempt.setPassed(passed);
        attempt.setTimeTakenSecs((int) Math.min(elapsed.toSeconds(), Integer.MAX_VALUE));
        attempt.setAttemptedAt(Date.from(now));
        attempt.setStatus(expired ? "EXPIRED" : "SUBMITTED");

        QuizAttempt saved = quizAttemptRepository.save(attempt);

        // Preserve the same side-effect as legacy create(): keep enrollment in sync.
        refreshEnrollmentProgress(saved.getStudentId(), saved.getCourseId());

        return toResponse(saved);
    }

    /**
     * Mark an {@code IN_PROGRESS} attempt as {@code ABANDONED}. Idempotent:
     * already-terminal attempts (SUBMITTED, ABANDONED, EXPIRED) silently no-op.
     */
    public void abandonAttempt(String studentId, String attemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("QuizAttempt not found: " + attemptId));

        if (!Objects.equals(attempt.getStudentId(), studentId)) {
            throw new ForbiddenException("Not your attempt");
        }

        String status = attempt.getStatus();
        if ("SUBMITTED".equals(status) || "ABANDONED".equals(status) || "EXPIRED".equals(status)) {
            return;  // already terminal
        }

        Instant now = Instant.now();
        if (attempt.getStartedAt() != null) {
            attempt.setTimeTakenSecs((int) Duration.between(attempt.getStartedAt(), now).toSeconds());
        }
        attempt.setStatus("ABANDONED");
        quizAttemptRepository.save(attempt);
    }

    // ── Legacy one-shot create (kept for backwards compatibility) ────────────

    /**
     * Legacy one-shot create — accepts client-supplied score/passed. Preserved
     * so existing FE that posts a finished attempt directly still works.
     *
     * @deprecated New code MUST use {@link #startAttempt(String, String)} +
     *             {@link #submitAttempt(String, String, List)}; only those
     *             enforce server-side grading and the timer.
     */
    @Deprecated
    public QuizAttemptResponse create(QuizAttemptRequest request) {
        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(request.getQuizId())
                .courseId(request.getCourseId())
                .studentId(request.getStudentId())
                .attemptedAt(new Date())
                .score(request.getScore())
                .passed(request.isPassed())
                .timeTakenSecs(request.getTimeTakenSecs())
                .answers(request.getAnswers() != null ? request.getAnswers() : new ArrayList<>())
                .build();
        QuizAttempt saved = quizAttemptRepository.save(attempt);
        refreshEnrollmentProgress(request.getStudentId(), request.getCourseId());
        return toResponse(saved);
    }

    public QuizAttemptResponse findById(String attemptId) {
        return toResponse(quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("QuizAttempt not found: " + attemptId)));
    }

    public List<QuizAttemptResponse> findByStudentId(String studentId) {
        return quizAttemptRepository.findByStudentId(studentId).stream().map(this::toResponse).toList();
    }

    public List<QuizAttemptResponse> findByQuizId(String quizId) {
        return quizAttemptRepository.findByQuizId(quizId).stream().map(this::toResponse).toList();
    }

    public List<QuizAttemptResponse> findByStudentAndQuiz(String studentId, String quizId) {
        return quizAttemptRepository.findByStudentIdAndQuizId(studentId, quizId)
                .stream().map(this::toResponse).toList();
    }

    public QuizAttemptResponse findLatestAttempt(String studentId, String quizId) {
        QuizAttempt attempt = quizAttemptRepository
                .findFirstByStudentIdAndQuizIdOrderByAttemptedAtDesc(studentId, quizId);
        if (attempt == null) throw new ResourceNotFoundException(
                "No attempt found for student " + studentId + " on quiz " + quizId);
        return toResponse(attempt);
    }

    public List<QuizAttemptResponse> findPassedByQuiz(String quizId) {
        return quizAttemptRepository.findByQuizIdAndPassed(quizId, true)
                .stream().map(this::toResponse).toList();
    }

    public double computeAvgScore(String quizId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizId(quizId);
        if (attempts.isEmpty()) return 0.0;
        double total = attempts.stream().mapToInt(QuizAttempt::getScore).sum();
        return Math.round((total / attempts.size()) * 10.0) / 10.0;
    }

    public boolean hasStudentPassedQuiz(String studentId, String quizId) {
        return quizAttemptRepository.existsByStudentIdAndQuizIdAndPassed(studentId, quizId, true);
    }

    public long countPassedByQuiz(String quizId) {
        return quizAttemptRepository.countByQuizIdAndPassed(quizId, true);
    }

    public void deleteById(String attemptId) {
        quizAttemptRepository.deleteById(attemptId);
    }

    /**
     * Sweeps IN_PROGRESS attempts past their deadline and flips them to EXPIRED.
     *
     * <p>A student who closes the tab past the deadline would otherwise stay
     * {@code IN_PROGRESS} forever, which breaks {@code findFirstBy...AndStatus}
     * idempotency on subsequent starts and corrupts course-progress maths.
     * This job runs every 5 minutes (with a 1-minute startup delay) and uses
     * a 30-second leniency window — strictly larger than {@link #GRACE_SECONDS}
     * so an in-flight legitimate submit never races the sweep.</p>
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void sweepExpiredAttempts() {
        List<QuizAttempt> stuck = quizAttemptRepository.findByStatus("IN_PROGRESS");
        Instant now = Instant.now();
        int flipped = 0;
        for (QuizAttempt a : stuck) {
            if (a.getStartedAt() == null) continue;
            ModuleQuiz quiz = moduleQuizRepository.findById(a.getQuizId()).orElse(null);
            if (quiz == null) continue;
            long durationSec = (long) quiz.getTimeLimit() * 60L;
            if (Duration.between(a.getStartedAt(), now).toSeconds() > durationSec + 30) {
                a.setStatus("EXPIRED");
                quizAttemptRepository.save(a);
                flipped++;
            }
        }
        if (flipped > 0) log.info("Expired sweep: flipped {} attempts", flipped);
    }

    // ── Server-side grading helpers ──────────────────────────────────────────

    /**
     * Grade each submitted answer against the canonical quiz definition.
     * Returns a fresh list — never mutates the client-supplied input — and
     * sets {@code correct} on each {@link Answer} based on the question's
     * {@code correctIdx} / {@code correctText}. Answers for unknown question
     * IDs are dropped (defensive: ignore noise rather than crash).
     */
    private List<Answer> gradeAnswers(ModuleQuiz quiz, List<Answer> clientAnswers) {
        Map<String, ModuleQuizQuestion> byId = new HashMap<>();
        if (quiz.getQuestions() != null) {
            for (ModuleQuizQuestion q : quiz.getQuestions()) {
                if (q != null && q.getId() != null) byId.put(q.getId(), q);
            }
        }

        List<Answer> graded = new ArrayList<>(clientAnswers.size());
        for (Answer in : clientAnswers) {
            if (in == null || in.getQuestionId() == null) continue;
            ModuleQuizQuestion q = byId.get(in.getQuestionId());
            if (q == null) continue;  // unknown question — drop silently

            boolean correct = false;
            if ("written".equalsIgnoreCase(q.getType())) {
                String expected = q.getCorrectText() == null ? "" : q.getCorrectText().trim();
                String given = in.getWrittenAnswer() == null ? "" : in.getWrittenAnswer().trim();
                correct = !expected.isEmpty() && expected.equalsIgnoreCase(given);
            } else {
                // default to multiple_choice
                correct = q.getCorrectIdx() != null && q.getCorrectIdx() == in.getSelectedIdx();
            }

            graded.add(Answer.builder()
                    .questionId(in.getQuestionId())
                    .selectedIdx(in.getSelectedIdx())
                    .writtenAnswer(in.getWrittenAnswer())
                    .correct(correct)
                    .build());
        }
        return graded;
    }

    /**
     * Sum the points of correctly-answered questions and express as a
     * percentage of total possible points. Returns {@code 0} when the quiz
     * has no scorable questions.
     */
    private int computeScore(ModuleQuiz quiz, List<Answer> gradedAnswers) {
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) return 0;

        Map<String, Integer> pointsByQ = new HashMap<>();
        int totalPoints = 0;
        for (ModuleQuizQuestion q : quiz.getQuestions()) {
            if (q == null || q.getId() == null) continue;
            int pts = Math.max(q.getPoints(), 0);
            pointsByQ.put(q.getId(), pts);
            totalPoints += pts;
        }
        if (totalPoints == 0) return 0;

        int earned = 0;
        for (Answer a : gradedAnswers) {
            if (a == null || !a.isCorrect()) continue;
            Integer pts = pointsByQ.get(a.getQuestionId());
            if (pts != null) earned += pts;
        }
        return (int) Math.round(100.0 * earned / totalPoints);
    }

    private QuizAttemptResponse toResponse(QuizAttempt a) {
        return QuizAttemptResponse.builder()
                .id(a.getId())
                .quizId(a.getQuizId())
                .courseId(a.getCourseId())
                .studentId(a.getStudentId())
                .attemptedAt(a.getAttemptedAt())
                .score(a.getScore())
                .passed(a.isPassed())
                .timeTakenSecs(a.getTimeTakenSecs())
                .answers(a.getAnswers())
                .build();
    }

    private void refreshEnrollmentProgress(String studentId, String courseId) {
        if (studentId == null || courseId == null) return;

        Course course = courseRepository.findById(courseId).orElse(null);
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).orElse(null);
        if (course == null || enrollment == null) return;

        int overallProgress = recalculateOverallProgress(course, enrollment);
        Update update = new Update()
                .set("overallProgress", overallProgress)
                .set("lastAccessedAt", new Date());
        if (overallProgress >= 100) {
            update.set("status", "completed").set("completedAt", new Date());
        }

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(enrollment.getId())),
                update,
                Enrollment.class
        );
    }

    private int recalculateOverallProgress(Course course, Enrollment enrollment) {
        Map<String, LectureProgressEntry> progressMap = new HashMap<>();
        if (enrollment.getLectureProgress() != null) {
            for (LectureProgressEntry entry : enrollment.getLectureProgress()) {
                if (entry != null && entry.getLectureId() != null) {
                    progressMap.put(entry.getLectureId(), entry);
                }
            }
        }

        int total = 0;
        int done = 0;

        if (course.getModules() != null) {
            for (CourseModule module : course.getModules()) {
                if (module.getLectures() == null) continue;
                for (Lecture lecture : module.getLectures()) {
                    total++;
                    LectureProgressEntry progress = progressMap.get(lecture.getId());
                    if (progress != null && progress.isCompleted()) done++;
                }
            }
        }

        Set<String> quizIds = new HashSet<>();
        moduleQuizRepository.findAllByCourseId(course.getId()).stream()
                .map(ModuleQuiz::getId)
                .filter(Objects::nonNull)
                .forEach(quizIds::add);
        if (course.getQuizId() != null) quizIds.add(course.getQuizId());

        for (String quizId : quizIds) {
            total++;
            List<QuizAttempt> attempts = quizAttemptRepository.findByStudentIdAndQuizId(enrollment.getStudentId(), quizId);
            if (attempts != null && !attempts.isEmpty()) done++;
        }

        return total == 0 ? 0 : (int) Math.round((double) done / total * 100);
    }
}
