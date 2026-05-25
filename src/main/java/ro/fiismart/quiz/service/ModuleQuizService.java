package ro.fiismart.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.ModuleQuiz;
import ro.fiismart.common.model.ModuleQuizQuestion;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.ModuleQuizRepository;
import ro.fiismart.common.util.AuthUtils;
import ro.fiismart.quiz.dto.modulequiz.CreateModuleQuizRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizQuestionRequest;
import ro.fiismart.quiz.dto.modulequiz.ModuleQuizResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for the three quiz flavours: lecture, module, course-final.
 *
 * Storage: a single {@code ModuleQuiz} collection keyed by id, with
 * (lectureId | moduleId | courseId) + a {@code quizScope} discriminator
 * disambiguating which slot a quiz fills.
 */
@Service
@RequiredArgsConstructor
public class ModuleQuizService {

    private static final String SCOPE_LECTURE = "lecture";
    private static final String SCOPE_MODULE = "module";
    private static final String SCOPE_COURSE_FINAL = "course_final";

    private final ModuleQuizRepository moduleQuizRepository;
    private final CourseRepository courseRepository;
    private final MongoTemplate mongoTemplate;

    // ── ALL QUIZZES FOR A COURSE ─────────────────────────────────────────────

    /** Returns every quiz attached to the course (lecture + module + course-final). */
    public List<ModuleQuizResponse> getAllQuizzesByCourse(String courseId) {
        ensureCourseExists(courseId);
        return moduleQuizRepository.findAllByCourseId(courseId).stream()
                .map(ModuleQuizResponse::fromModel)
                .collect(Collectors.toList());
    }

    // ── LECTURE QUIZ ─────────────────────────────────────────────────────────

public ModuleQuizResponse createOrUpdateLectureQuiz(String courseId, String moduleId,
                                                         String lectureId,
                                                         CreateModuleQuizRequest req) {
        verifyCourseOwner(courseId);
        moduleQuizRepository.deleteByLectureIdAndQuizScope(lectureId, SCOPE_LECTURE);
        ModuleQuiz saved = moduleQuizRepository.save(
                buildQuiz(courseId, moduleId, lectureId, SCOPE_LECTURE, req));
        bumpCourseUpdatedAt(courseId);
        return ModuleQuizResponse.fromModel(saved);
    }

    public ModuleQuizResponse getLectureQuiz(String courseId, String moduleId, String lectureId) {
        return ModuleQuizResponse.fromModel(getLectureQuizOrThrow(lectureId));
    }

    public void deleteLectureQuiz(String courseId, String moduleId, String lectureId) {
        verifyCourseOwner(courseId);
        if (!moduleQuizRepository.existsByLectureIdAndQuizScope(lectureId, SCOPE_LECTURE)) {
            throw new ResourceNotFoundException("Quiz not found for lecture: " + lectureId);
        }
        moduleQuizRepository.deleteByLectureIdAndQuizScope(lectureId, SCOPE_LECTURE);
        bumpCourseUpdatedAt(courseId);
    }

    public ModuleQuizResponse addQuestionToLectureQuiz(String courseId, String moduleId,
                                                       String lectureId,
                                                       ModuleQuizQuestionRequest req) {
        verifyCourseOwner(courseId);
        ModuleQuiz quiz = getLectureQuizOrThrow(lectureId);
        appendQuestion(quiz, req);
        bumpCourseUpdatedAt(courseId);
        return ModuleQuizResponse.fromModel(moduleQuizRepository.findById(quiz.getId()).orElseThrow());
    }

    public void removeQuestionFromLectureQuiz(String courseId, String moduleId,
                                              String lectureId, String questionId) {
        verifyCourseOwner(courseId);
        ModuleQuiz quiz = getLectureQuizOrThrow(lectureId);
        removeQuestion(quiz, questionId);
        bumpCourseUpdatedAt(courseId);
    }

    public ModuleQuizResponse reorderLectureQuizQuestions(String courseId, String moduleId,
                                                          String lectureId,
                                                          List<String> orderedQuestionIds) {
        verifyCourseOwner(courseId);
        ModuleQuiz quiz = getLectureQuizOrThrow(lectureId);
        applyReorder(quiz, orderedQuestionIds);
        bumpCourseUpdatedAt(courseId);
        return ModuleQuizResponse.fromModel(moduleQuizRepository.findById(quiz.getId()).orElseThrow());
    }

    // ── MODULE QUIZ ──────────────────────────────────────────────────────────

    public ModuleQuizResponse createOrUpdateModuleQuiz(String courseId, String moduleId,
                                                       CreateModuleQuizRequest req) {
        verifyCourseOwner(courseId);
        moduleQuizRepository.deleteByModuleIdAndQuizScope(moduleId, SCOPE_MODULE);
        ModuleQuiz saved = moduleQuizRepository.save(
                buildQuiz(courseId, moduleId, null, SCOPE_MODULE, req));
        bumpCourseUpdatedAt(courseId);
        return ModuleQuizResponse.fromModel(saved);
    }

    public ModuleQuizResponse getModuleQuiz(String courseId, String moduleId) {
        return ModuleQuizResponse.fromModel(getModuleQuizOrThrow(moduleId));
    }

    public void deleteModuleQuiz(String courseId, String moduleId) {
        verifyCourseOwner(courseId);
        if (!moduleQuizRepository.existsByModuleIdAndQuizScope(moduleId, SCOPE_MODULE)) {
            throw new ResourceNotFoundException("Quiz not found for module: " + moduleId);
        }
        moduleQuizRepository.deleteByModuleIdAndQuizScope(moduleId, SCOPE_MODULE);
        bumpCourseUpdatedAt(courseId);
    }

    public ModuleQuizResponse addQuestionToModuleQuiz(String courseId, String moduleId,
                                                      ModuleQuizQuestionRequest req) {
        verifyCourseOwner(courseId);
        ModuleQuiz quiz = getModuleQuizOrThrow(moduleId);
        appendQuestion(quiz, req);
        bumpCourseUpdatedAt(courseId);
        return ModuleQuizResponse.fromModel(moduleQuizRepository.findById(quiz.getId()).orElseThrow());
    }

    public void removeQuestionFromModuleQuiz(String courseId, String moduleId, String questionId) {
        verifyCourseOwner(courseId);
        ModuleQuiz quiz = getModuleQuizOrThrow(moduleId);
        removeQuestion(quiz, questionId);
        bumpCourseUpdatedAt(courseId);
    }

    public ModuleQuizResponse reorderModuleQuizQuestions(String courseId, String moduleId,
                                                         List<String> orderedQuestionIds) {
        verifyCourseOwner(courseId);
        ModuleQuiz quiz = getModuleQuizOrThrow(moduleId);
        applyReorder(quiz, orderedQuestionIds);
        bumpCourseUpdatedAt(courseId);
        return ModuleQuizResponse.fromModel(moduleQuizRepository.findById(quiz.getId()).orElseThrow());
    }

    // ── COURSE-FINAL QUIZ ────────────────────────────────────────────────────

public ModuleQuizResponse createOrUpdateCourseFinalQuiz(String courseId,
                                                              CreateModuleQuizRequest req) {
        verifyCourseOwner(courseId);
        moduleQuizRepository.deleteByCourseIdAndQuizScope(courseId, SCOPE_COURSE_FINAL);
        ModuleQuiz saved = moduleQuizRepository.save(
                buildQuiz(courseId, null, null, SCOPE_COURSE_FINAL, req));
        bumpCourseUpdatedAt(courseId);
        return ModuleQuizResponse.fromModel(saved);
    }

    public ModuleQuizResponse getCourseFinalQuiz(String courseId) {
        return ModuleQuizResponse.fromModel(getCourseFinalQuizOrThrow(courseId));
    }

    public void deleteCourseFinalQuiz(String courseId) {
        verifyCourseOwner(courseId);
        if (!moduleQuizRepository.existsByCourseIdAndQuizScope(courseId, SCOPE_COURSE_FINAL)) {
            throw new ResourceNotFoundException("Final quiz not found for course: " + courseId);
        }
        moduleQuizRepository.deleteByCourseIdAndQuizScope(courseId, SCOPE_COURSE_FINAL);
        bumpCourseUpdatedAt(courseId);
    }

public ModuleQuizResponse addQuestionToCourseFinalQuiz(String courseId,
                                                             ModuleQuizQuestionRequest req) {
        verifyCourseOwner(courseId);
        ModuleQuiz quiz = getCourseFinalQuizOrThrow(courseId);
        appendQuestion(quiz, req);
        bumpCourseUpdatedAt(courseId);
        return ModuleQuizResponse.fromModel(moduleQuizRepository.findById(quiz.getId()).orElseThrow());
    }

    public void removeQuestionFromCourseFinalQuiz(String courseId, String questionId) {
        verifyCourseOwner(courseId);
        ModuleQuiz quiz = getCourseFinalQuizOrThrow(courseId);
        removeQuestion(quiz, questionId);
        bumpCourseUpdatedAt(courseId);
    }

public ModuleQuizResponse reorderCourseFinalQuizQuestions(String courseId,
                                                                List<String> orderedQuestionIds) {
        verifyCourseOwner(courseId);
        ModuleQuiz quiz = getCourseFinalQuizOrThrow(courseId);
        applyReorder(quiz, orderedQuestionIds);
        bumpCourseUpdatedAt(courseId);
        return ModuleQuizResponse.fromModel(moduleQuizRepository.findById(quiz.getId()).orElseThrow());
    }

    // ── INTERNALS ────────────────────────────────────────────────────────────

    private void verifyCourseOwner(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        String currentUserId = AuthUtils.getCurrentUserId();
        if (course.getTeacherId() == null || !course.getTeacherId().equals(currentUserId)) {
            throw new ForbiddenException("You are not the owner of this course");
        }
    }

    private ModuleQuiz buildQuiz(String courseId, String moduleId, String lectureId,
                                 String quizScope, CreateModuleQuizRequest req) {
        List<ModuleQuizQuestion> questions = req.getQuestions() != null
                ? req.getQuestions().stream().map(this::toQuestion).collect(Collectors.toList())
                : new ArrayList<>();

        return ModuleQuiz.builder()
                .courseId(courseId)
                .moduleId(moduleId)
                .lectureId(lectureId)
                .quizScope(quizScope)
                .title(req.getTitle())
                .passingScore(req.getPassingScore())
                .timeLimit(req.getTimeLimit())
                .shuffleQuestions(req.isShuffleQuestions())
                .questions(questions)
                .build();
    }

    private ModuleQuizQuestion toQuestion(ModuleQuizQuestionRequest req) {
        return ModuleQuizQuestion.builder()
                .id(UUID.randomUUID().toString())
                .text(req.getText())
                .imageUrl(req.getImageUrl())
                .type(req.getType())
                .points(req.getPoints())
                .options(req.getOptions())
                .correctIdx(req.getCorrectIdx())
                .correctText(req.getCorrectText())
                .explanation(req.getExplanation())
                .build();
    }

    private void appendQuestion(ModuleQuiz quiz, ModuleQuizQuestionRequest req) {
        ModuleQuizQuestion q = toQuestion(req);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(quiz.getId())),
                new Update().push("questions", q),
                ModuleQuiz.class);
    }

    private void removeQuestion(ModuleQuiz quiz, String questionId) {
        boolean exists = quiz.getQuestions() != null
                && quiz.getQuestions().stream().anyMatch(q -> questionId.equals(q.getId()));
        if (!exists) {
            throw new ResourceNotFoundException("Question not found: " + questionId);
        }
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(quiz.getId())),
                new Update().pull("questions", new org.bson.Document("id", questionId)),
                ModuleQuiz.class);
    }

    private void applyReorder(ModuleQuiz quiz, List<String> orderedIds) {
        List<ModuleQuizQuestion> existing = quiz.getQuestions() != null
                ? quiz.getQuestions() : new ArrayList<>();
        if (orderedIds.size() != existing.size()) {
            throw new IllegalArgumentException(
                    "Numarul de ID-uri nu corespunde cu numarul de intrebari");
        }
        List<ModuleQuizQuestion> reordered = new ArrayList<>();
        for (String qid : orderedIds) {
            ModuleQuizQuestion found = existing.stream()
                    .filter(q -> qid.equals(q.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + qid));
            reordered.add(found);
        }
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(quiz.getId())),
                new Update().set("questions", reordered),
                ModuleQuiz.class);
    }

    private void ensureCourseExists(String courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }
    }

    private ModuleQuiz getLectureQuizOrThrow(String lectureId) {
        return moduleQuizRepository.findByLectureIdAndQuizScope(lectureId, SCOPE_LECTURE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quiz not found for lecture: " + lectureId));
    }

    private ModuleQuiz getModuleQuizOrThrow(String moduleId) {
        return moduleQuizRepository.findByModuleIdAndQuizScope(moduleId, SCOPE_MODULE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quiz not found for module: " + moduleId));
    }

    private ModuleQuiz getCourseFinalQuizOrThrow(String courseId) {
        return moduleQuizRepository.findByCourseIdAndQuizScope(courseId, SCOPE_COURSE_FINAL)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Final quiz not found for course: " + courseId));
    }

    private void bumpCourseUpdatedAt(String courseId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId)),
                new Update().set("updatedAt", new Date()),
                Course.class);
    }
}
