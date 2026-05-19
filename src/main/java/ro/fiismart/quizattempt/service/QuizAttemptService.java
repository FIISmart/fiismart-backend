package ro.fiismart.quizattempt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleQuizRepository moduleQuizRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Creates a quiz attempt. The score and pass/fail are computed server-side
     * from the submitted {@code answers} against the canonical quiz definition.
     * The {@code score} / {@code passed} / {@code correct} fields supplied by
     * the client are <b>ignored</b>. The {@code studentId} is passed in by the
     * controller from the authenticated principal — the request body field is
     * also ignored.
     */
    public QuizAttemptResponse create(String studentId, QuizAttemptRequest request) {
        ModuleQuiz quiz = moduleQuizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quiz not found: " + request.getQuizId()));

        List<Answer> submitted = request.getAnswers() != null
                ? request.getAnswers() : new ArrayList<>();
        List<Answer> graded = gradeAnswers(quiz, submitted);

        int passingScore = quiz.getPassingScore() > 0 ? quiz.getPassingScore() : 70;
        int score = computeScore(graded);
        boolean passed = score >= passingScore;

        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(request.getQuizId())
                .courseId(request.getCourseId())
                .studentId(studentId)
                .attemptedAt(new Date())
                .score(score)
                .passed(passed)
                .timeTakenSecs(request.getTimeTakenSecs())
                .answers(graded)
                .build();
        QuizAttempt saved = quizAttemptRepository.save(attempt);
        refreshEnrollmentProgress(studentId, request.getCourseId());
        return toResponse(saved);
    }

    /**
     * Compares each submitted answer against the canonical quiz question and
     * stamps {@link Answer#setCorrect(boolean)} based on the question's type.
     * Returns a new list (never mutates the input). Submitted answers whose
     * {@code questionId} doesn't match any quiz question are kept but marked
     * incorrect.
     */
    private List<Answer> gradeAnswers(ModuleQuiz quiz, List<Answer> submitted) {
        Map<String, ModuleQuizQuestion> byId = new HashMap<>();
        if (quiz.getQuestions() != null) {
            for (ModuleQuizQuestion q : quiz.getQuestions()) {
                if (q != null && q.getId() != null) byId.put(q.getId(), q);
            }
        }
        List<Answer> graded = new ArrayList<>(submitted.size());
        for (Answer a : submitted) {
            ModuleQuizQuestion q = a != null ? byId.get(a.getQuestionId()) : null;
            boolean correct = false;
            if (q != null && a != null) {
                if ("written".equalsIgnoreCase(q.getType())) {
                    String expected = q.getCorrectText();
                    String got = a.getWrittenAnswer();
                    correct = expected != null && got != null
                            && expected.trim().equalsIgnoreCase(got.trim());
                } else {
                    correct = q.getCorrectIdx() != null
                            && a.getSelectedIdx() == q.getCorrectIdx();
                }
            }
            graded.add(Answer.builder()
                    .questionId(a != null ? a.getQuestionId() : null)
                    .selectedIdx(a != null ? a.getSelectedIdx() : -1)
                    .writtenAnswer(a != null ? a.getWrittenAnswer() : null)
                    .correct(correct)
                    .build());
        }
        return graded;
    }

    private int computeScore(List<Answer> graded) {
        if (graded == null || graded.isEmpty()) return 0;
        long correct = graded.stream().filter(Answer::isCorrect).count();
        return (int) Math.round((correct * 100.0) / graded.size());
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
