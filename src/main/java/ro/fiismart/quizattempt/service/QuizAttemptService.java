package ro.fiismart.quizattempt.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.fiismart.ai.dto.response.FreeTextGradeResult;
import ro.fiismart.ai.service.AiTextGraderService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    /** Wall-clock slack on the deadline so a borderline-late submit still counts. */
    private static final int GRACE_SECONDS = 5;

    /**
     * Maximum total wall-clock time we allow for parallel AI grading across
     * all free_text questions in one submission. If we miss the budget, the
     * remaining questions are submitted with a null score (treated as
     * incorrect) — the rest of the quiz still grades and persists.
     */
    private static final long AI_GRADING_TIMEOUT_SECONDS = 30L;

    /**
     * Per-grade wall-clock cap. Strictly shorter than the overall
     * AI_GRADING_TIMEOUT_SECONDS so a single slow Gemini call can't burn
     * the whole budget — each question falls back individually on
     * timeout. Picked at 20s = (overall budget − one round-trip slack).
     */
    private static final long AI_PER_GRADE_TIMEOUT_SECONDS = 20L;

    /** Default pass threshold for free-text questions that omit one explicitly. */
    private static final int DEFAULT_FREE_TEXT_PASS_THRESHOLD = 70;

    /** Minimum AI confidence below which we refuse to count an answer correct. */
    private static final double FREE_TEXT_MIN_CONFIDENCE = 0.7;

    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleQuizRepository moduleQuizRepository;
    private final MongoTemplate mongoTemplate;
    private final AiTextGraderService aiTextGraderService;

    /**
     * Dedicated bounded executor for AI free-text grading.
     *
     * <p>We previously dispatched grading futures to {@code ForkJoinPool.commonPool()}.
     * That's shared with every other parallel-stream consumer in the JVM, and a
     * {@code CompletableFuture.cancel(true)} only flips the future's state — the
     * underlying blocking Gemini HTTP call keeps running, occupying a commonPool
     * worker for the whole upstream socket timeout. Under load that can starve
     * unrelated work.</p>
     *
     * <p>Sized at max(4, availableProcessors()) daemon threads. Daemons so JVM
     * shutdown doesn't have to wait for in-flight grading; explicit shutdown via
     * {@link #shutdownAiGrading()} for clean context reload.</p>
     */
    private final ExecutorService aiGradingExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            new java.util.concurrent.ThreadFactory() {
                private final AtomicInteger seq = new AtomicInteger();
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ai-grading-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });

    @PreDestroy
    void shutdownAiGrading() {
        aiGradingExecutor.shutdownNow();
    }

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
                .score(request.getScore() != null ? request.getScore() : 0)
                .passed(Boolean.TRUE.equals(request.getPassed()))
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
     * sets {@code correct} on each {@link Answer}. Answers for unknown
     * question IDs are dropped (defensive: ignore noise rather than crash).
     *
     * <p>Grading routes by question {@code type}:
     * <ul>
     *   <li>{@code multiple_choice} → index match (synchronous, instant).</li>
     *   <li>{@code written} (legacy) → case-insensitive trim match
     *       (synchronous, instant).</li>
     *   <li>{@code free_text} → AI rubric grading via
     *       {@link AiTextGraderService}. Each free-text question is graded
     *       on a {@link CompletableFuture}; the whole batch is awaited up
     *       to {@value #AI_GRADING_TIMEOUT_SECONDS}s. Any question that does
     *       not complete in time is recorded with a null AI score (treated
     *       as incorrect) so the rest of the submission still goes through.
     *       The submission itself NEVER fails because AI grading failed.</li>
     * </ul></p>
     */
    private List<Answer> gradeAnswers(ModuleQuiz quiz, List<Answer> clientAnswers) {
        Map<String, ModuleQuizQuestion> byId = new HashMap<>();
        if (quiz.getQuestions() != null) {
            for (ModuleQuizQuestion q : quiz.getQuestions()) {
                if (q != null && q.getId() != null) byId.put(q.getId(), q);
            }
        }

        // Two passes: synchronous fast lane for MC + legacy written, then
        // async fan-out for any free_text answers. Output order matches
        // client input so downstream code (and UI) stays predictable.
        List<Answer> graded = new ArrayList<>(clientAnswers.size());
        // slot index in `graded` → (future, originalInputAnswer, question)
        List<PendingFreeText> pending = new ArrayList<>();

        for (Answer in : clientAnswers) {
            if (in == null || in.getQuestionId() == null) continue;
            ModuleQuizQuestion q = byId.get(in.getQuestionId());
            if (q == null) continue;  // unknown question — drop silently

            String type = q.getType() == null ? "" : q.getType();
            if ("free_text".equalsIgnoreCase(type)) {
                // Placeholder slot — we'll splice in the future's result
                // after the parallel grading wave below.
                graded.add(null);
                int slot = graded.size() - 1;
                // Run on our dedicated bounded executor so a slow Gemini
                // call can't pollute the JVM-wide commonPool. orTimeout
                // converts a per-grade hang into a fallback Answer at
                // AI_PER_GRADE_TIMEOUT_SECONDS, well inside the overall
                // budget, instead of starving siblings that finish fast.
                Answer originalInput = in;
                ModuleQuizQuestion question = q;
                CompletableFuture<Answer> fut = CompletableFuture
                        .supplyAsync(() -> gradeFreeText(question, originalInput), aiGradingExecutor)
                        .orTimeout(AI_PER_GRADE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .exceptionally(t -> {
                            log.warn("AI grading per-grade timed out / failed (questionId={}): {}",
                                    question.getId(), t.getClass().getSimpleName());
                            return fallbackFreeTextAnswer(originalInput, question);
                        });
                pending.add(new PendingFreeText(slot, fut, in, q));
            } else if ("written".equalsIgnoreCase(type)) {
                String expected = q.getCorrectText() == null ? "" : q.getCorrectText().trim();
                String given = in.getWrittenAnswer() == null ? "" : in.getWrittenAnswer().trim();
                boolean correct = !expected.isEmpty() && expected.equalsIgnoreCase(given);
                graded.add(Answer.builder()
                        .questionId(in.getQuestionId())
                        .selectedIdx(in.getSelectedIdx())
                        .writtenAnswer(in.getWrittenAnswer())
                        .correct(correct)
                        .build());
            } else {
                // default to multiple_choice
                boolean correct = q.getCorrectIdx() != null && q.getCorrectIdx() == in.getSelectedIdx();
                graded.add(Answer.builder()
                        .questionId(in.getQuestionId())
                        .selectedIdx(in.getSelectedIdx())
                        .writtenAnswer(in.getWrittenAnswer())
                        .correct(correct)
                        .build());
            }
        }

        if (!pending.isEmpty()) {
            awaitFreeTextGrading(pending, graded);
        }
        return graded;
    }

    /**
     * Pairs a free-text grading future with the slot it should land in and
     * the original input — needed to build a fallback answer if the future
     * times out or fails. Local helper, not part of the public API.
     */
    private record PendingFreeText(
            int slot,
            CompletableFuture<Answer> future,
            Answer originalInput,
            ModuleQuizQuestion question) {}

    /**
     * Awaits the parallel free-text grading wave with a hard wall-clock
     * budget. On timeout / interruption, any still-pending future is
     * replaced with a null-score fallback so the submission can complete.
     *
     * <p>The submission is sacred: this method MUST NOT throw any
     * AI-related exception. All failure modes are converted to a fallback
     * {@link Answer} with {@code correct=false} and null AI score.</p>
     */
    private void awaitFreeTextGrading(List<PendingFreeText> pending, List<Answer> graded) {
        CompletableFuture<Void> all = CompletableFuture.allOf(
                pending.stream().map(PendingFreeText::future).toArray(CompletableFuture[]::new));

        try {
            all.get(AI_GRADING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            log.warn("AI grading timed out after {}s — {} question(s) still pending",
                    AI_GRADING_TIMEOUT_SECONDS, pending.size());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("AI grading interrupted — falling back to null scores");
        } catch (ExecutionException ee) {
            // Individual future failures are caught inside gradeFreeText;
            // an EE here means an unexpected wiring bug — log and continue.
            log.warn("AI grading aggregate failed unexpectedly: {}",
                    ee.getCause() != null ? ee.getCause().getClass().getSimpleName() : "unknown");
        }

        for (PendingFreeText p : pending) {
            CompletableFuture<Answer> f = p.future();
            Answer resolved = null;
            if (f.isDone() && !f.isCompletedExceptionally() && !f.isCancelled()) {
                try {
                    resolved = f.getNow(null);
                } catch (Exception ex) {
                    log.warn("AI grading slot={} resolve failed: {}", p.slot(), ex.getClass().getSimpleName());
                }
            } else {
                // Still pending or failed — cancel best-effort and fall back.
                f.cancel(true);
            }
            if (resolved == null) {
                resolved = fallbackFreeTextAnswer(p.originalInput(), p.question());
            }
            graded.set(p.slot(), resolved);
        }
    }

    /**
     * Runs a single free-text rubric grading. Wraps the AI grader's result
     * onto an {@link Answer} that carries both the raw AI verdict and the
     * derived boolean {@code correct} = (score ≥ threshold ∧ confidence ≥ 0.7).
     */
    private Answer gradeFreeText(ModuleQuizQuestion question, Answer in) {
        int threshold = question.getPassThreshold() == null
                ? DEFAULT_FREE_TEXT_PASS_THRESHOLD
                : question.getPassThreshold();

        FreeTextGradeResult result;
        try {
            result = aiTextGraderService.grade(
                    question.getText(),
                    question.getSampleAnswer(),
                    question.getKeyConcepts(),
                    in.getWrittenAnswer());
        } catch (Exception e) {
            // AiTextGraderService promises not to throw, but defend in depth.
            log.warn("AI grading threw unexpectedly for questionId={}: {}",
                    question.getId(), e.getClass().getSimpleName());
            return fallbackFreeTextAnswer(in, question);
        }

        Double score = result == null ? null : result.score();
        Double confidence = result == null ? null : result.confidence();
        boolean correct = score != null
                && confidence != null
                && score >= threshold
                && confidence >= FREE_TEXT_MIN_CONFIDENCE;

        return Answer.builder()
                .questionId(in.getQuestionId())
                .selectedIdx(in.getSelectedIdx())
                .writtenAnswer(in.getWrittenAnswer())
                .correct(correct)
                .aiScore(score)
                .aiConfidence(confidence)
                .aiReasoning(result == null ? null : result.reasoning())
                .aiMissingConcepts(result == null ? null : result.missingConcepts())
                .build();
    }

    /**
     * Emit an Answer for a free-text question whose AI grading we could not
     * complete (timeout / unexpected failure). Keeps the user's text on the
     * record but marks the question incorrect with a null score so the UI
     * can show "Evaluare AI indisponibilă".
     */
    private Answer fallbackFreeTextAnswer(Answer in, ModuleQuizQuestion q) {
        return Answer.builder()
                .questionId(in == null ? (q == null ? null : q.getId()) : in.getQuestionId())
                .selectedIdx(in == null ? 0 : in.getSelectedIdx())
                .writtenAnswer(in == null ? null : in.getWrittenAnswer())
                .correct(false)
                .aiScore(null)
                .aiConfidence(null)
                .aiReasoning(null)
                .aiMissingConcepts(null)
                .build();
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
