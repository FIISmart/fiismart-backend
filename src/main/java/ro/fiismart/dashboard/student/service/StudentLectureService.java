package ro.fiismart.dashboard.student.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.*;
import ro.fiismart.common.repository.*;
import ro.fiismart.dashboard.student.dto.*;

import java.util.*;

@Service
public class StudentLectureService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final ModuleQuizRepository moduleQuizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final StudentQuizService studentQuizService;
    private final MongoTemplate mongoTemplate;

    public StudentLectureService(CourseRepository courseRepository,
                                 EnrollmentRepository enrollmentRepository,
                                 QuizRepository quizRepository,
                                 ModuleQuizRepository moduleQuizRepository,
                                 QuizAttemptRepository quizAttemptRepository,
                                 StudentQuizService studentQuizService,
                                 MongoTemplate mongoTemplate) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.moduleQuizRepository = moduleQuizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.studentQuizService = studentQuizService;
        this.mongoTemplate = mongoTemplate;
    }

    public List<StudentModuleDTO> getModules(String studentId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        List<CourseModule> modules = course.getModules() != null ? course.getModules() : new ArrayList<>();
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).orElse(null);
        Map<String, LectureProgressEntry> progressMap = buildProgressMap(enrollment);

        List<StudentModuleDTO> result = new ArrayList<>();
        for (CourseModule module : modules) {
            StudentModuleDTO dto = new StudentModuleDTO();
            dto.setModuleId(module.getId());
            dto.setTitle(module.getTitle());
            dto.setDescription(module.getDescription());
            dto.setOrder(module.getOrder());

            List<StudentLectureDTO> lectureDtos = new ArrayList<>();
            List<Lecture> lectures = module.getLectures() != null ? module.getLectures() : new ArrayList<>();
            for (Lecture lecture : lectures) {
                LectureProgressEntry progress = progressMap.get(lecture.getId());
                lectureDtos.add(buildLectureDTO(lecture, progress, studentId));
            }
            lectureDtos.sort(Comparator.comparingInt(StudentLectureDTO::getOrder));
            dto.setLectures(lectureDtos);

            populateModuleQuizInfo(dto, module, studentId);
            result.add(dto);
        }

        result.sort(Comparator.comparingInt(StudentModuleDTO::getOrder));
        return result;
    }

    public StudentLectureDetailDTO getLectureDetail(String studentId, String courseId, String lectureId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        Lecture lecture = findLectureInCourse(course, lectureId);
        if (lecture == null) throw new ResourceNotFoundException("Lecture", lectureId);

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).orElse(null);
        LectureProgressEntry progress = findLatestProgress(enrollment, lectureId);

        StudentLectureDetailDTO dto = new StudentLectureDetailDTO();
        dto.setLectureId(lecture.getId());
        dto.setTitle(lecture.getTitle());
        dto.setType(resolveLectureType(lecture));
        dto.setContent(resolveLectureContent(lecture));
        dto.setVideoUrl(lecture.getVideoUrl());
        dto.setPdfUrl(resolvePdfUrl(lecture));
        dto.setImageUrls(lecture.getImageUrls());
        dto.setOrder(lecture.getOrder());
        dto.setDurationSecs(lecture.getDurationSecs());
        dto.setPublishedAt(lecture.getPublishedAt());
        moduleQuizRepository.findByLectureIdAndQuizScope(lecture.getId(), "lecture")
                .ifPresent(quiz -> dto.setQuiz(studentQuizService.buildStatus(
                        quiz.getId(), quiz.getTitle(), quiz.getQuizScope(),
                        quiz.getModuleId(), quiz.getLectureId(), studentId)));

        if (progress != null) {
            dto.setWatchedPercent(progress.getWatchedPercent());
            dto.setPositionSecs(progress.getPositionSecs());
            dto.setCompleted(progress.isCompleted());
        }
        return dto;
    }

    public StudentLectureProgressResponse updateLectureProgress(String studentId, String courseId,
                                                                String lectureId,
                                                                StudentLectureProgressRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        Lecture lecture = findLectureInCourse(course, lectureId);
        if (lecture == null) throw new ResourceNotFoundException("Lecture", lectureId);
        updateLectureDurationIfNeeded(courseId, course, lecture, request.getDurationSecs());

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", studentId + "@" + courseId));

        int watchedPercent = request.getWatchedPercent();
        if (watchedPercent < 0) watchedPercent = 0;
        if (watchedPercent > 100) watchedPercent = 100;

        int positionSecs = Math.max(0, request.getPositionSecs());
        if (lecture.getDurationSecs() > 0 && positionSecs > lecture.getDurationSecs()) {
            positionSecs = lecture.getDurationSecs();
        }

        LectureProgressEntry existingProgress = findLatestProgress(enrollment, lectureId);

        boolean completed = request.isCompleted() || watchedPercent >= 95;

        if (existingProgress != null) {
            watchedPercent = Math.max(watchedPercent, existingProgress.getWatchedPercent());
            completed = existingProgress.isCompleted() || completed;
        }

        Date now = new Date();

        LectureProgressEntry progressEntry = LectureProgressEntry.builder()
                .lectureId(lectureId)
                .moduleId(lecture.getModuleId())
                .watchedPercent(watchedPercent)
                .positionSecs(positionSecs)
                .completed(completed)
                .updatedAt(now)
                .build();

        upsertLectureProgress(enrollment.getId(), lectureId, progressEntry);

        Enrollment refreshed = enrollmentRepository.findById(enrollment.getId()).orElse(enrollment);
        int overallProgress = recalculateOverallProgress(course, refreshed);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(enrollment.getId())),
                new Update().set("overallProgress", overallProgress).set("lastAccessedAt", now),
                Enrollment.class
        );

        String enrollmentStatus = enrollment.getStatus();
        boolean courseCompleted = false;
        if (overallProgress >= 100) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(enrollment.getId())),
                    new Update().set("status", "completed").set("completedAt", now),
                    Enrollment.class
            );
            enrollmentStatus = "completed";
            courseCompleted = true;
        }

        StudentLectureProgressResponse response = new StudentLectureProgressResponse();
        response.setLectureId(lectureId);
        response.setWatchedPercent(watchedPercent);
        response.setPositionSecs(positionSecs);
        response.setCompleted(completed);
        response.setOverallProgress(overallProgress);
        response.setEnrollmentStatus(enrollmentStatus);
        response.setCourseCompleted(courseCompleted);
        return response;
    }

    private void upsertLectureProgress(String enrollmentId, String lectureId, LectureProgressEntry entry) {
        // Try to update existing progress entry
        com.mongodb.client.result.UpdateResult result = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(enrollmentId)
                        .and("lectureProgress.lectureId").is(lectureId)),
                new Update().set("lectureProgress.$", entry),
                Enrollment.class
        );

        // If not found, push new entry
        if (result.getMatchedCount() == 0) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(enrollmentId)),
                    new Update().push("lectureProgress", entry),
                    Enrollment.class
            );
        }
    }

    private int recalculateOverallProgress(Course course, Enrollment enrollment) {
        Map<String, LectureProgressEntry> progressMap = buildProgressMap(enrollment);
        int total = 0;
        int done = 0;

        if (course.getModules() != null) {
            for (CourseModule module : course.getModules()) {
                if (module.getLectures() == null) continue;
                for (Lecture lecture : module.getLectures()) {
                    total++;
                    LectureProgressEntry p = progressMap.get(lecture.getId());
                    if (p != null && p.isCompleted()) done++;
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

    private Map<String, LectureProgressEntry> buildProgressMap(Enrollment enrollment) {
        Map<String, LectureProgressEntry> map = new HashMap<>();
        if (enrollment != null && enrollment.getLectureProgress() != null) {
            for (LectureProgressEntry p : enrollment.getLectureProgress()) {
                if (p.getLectureId() == null) continue;
                LectureProgressEntry existing = map.get(p.getLectureId());
                if (existing == null || isNewer(p, existing)) {
                    map.put(p.getLectureId(), p);
                }
            }
        }
        return map;
    }

    private LectureProgressEntry findLatestProgress(Enrollment enrollment, String lectureId) {
        if (enrollment == null || enrollment.getLectureProgress() == null) return null;

        LectureProgressEntry latest = null;
        for (LectureProgressEntry entry : enrollment.getLectureProgress()) {
            if (entry == null || !lectureId.equals(entry.getLectureId())) continue;
            if (latest == null || isNewer(entry, latest)) {
                latest = entry;
            }
        }
        return latest;
    }

    private boolean isNewer(LectureProgressEntry candidate, LectureProgressEntry current) {
        Date candidateUpdatedAt = candidate != null ? candidate.getUpdatedAt() : null;
        Date currentUpdatedAt = current != null ? current.getUpdatedAt() : null;

        if (candidateUpdatedAt == null && currentUpdatedAt == null) return false;
        if (candidateUpdatedAt == null) return false;
        if (currentUpdatedAt == null) return true;
        return candidateUpdatedAt.after(currentUpdatedAt);
    }


    private StudentLectureDTO buildLectureDTO(Lecture lecture, LectureProgressEntry progress, String studentId) {
        StudentLectureDTO dto = new StudentLectureDTO();
        dto.setLectureId(lecture.getId());
        dto.setTitle(lecture.getTitle());
        dto.setType(resolveLectureType(lecture));
        dto.setContent(resolveLectureContent(lecture));
        dto.setVideoUrl(lecture.getVideoUrl());
        dto.setPdfUrl(resolvePdfUrl(lecture));
        dto.setOrder(lecture.getOrder());
        dto.setDurationSecs(lecture.getDurationSecs());
        if (progress != null) {
            dto.setWatchedPercent(progress.getWatchedPercent());
            dto.setPositionSecs(progress.getPositionSecs());
            dto.setCompleted(progress.isCompleted());
        }
        moduleQuizRepository.findByLectureIdAndQuizScope(lecture.getId(), "lecture")
                .ifPresent(quiz -> dto.setQuiz(studentQuizService.buildStatus(
                        quiz.getId(), quiz.getTitle(), quiz.getQuizScope(),
                        quiz.getModuleId(), quiz.getLectureId(), studentId)));
        return dto;
    }

    private void populateModuleQuizInfo(StudentModuleDTO dto, CourseModule module, String studentId) {
        ModuleQuiz moduleQuiz = moduleQuizRepository.findByModuleIdAndQuizScope(module.getId(), "module").orElse(null);
        if (moduleQuiz != null) {
            StudentQuizStatusDTO status = studentQuizService.buildStatus(
                    moduleQuiz.getId(), moduleQuiz.getTitle(), moduleQuiz.getQuizScope(),
                    moduleQuiz.getModuleId(), moduleQuiz.getLectureId(), studentId);
            dto.setQuiz(status);
            dto.setHasQuiz(true);
            dto.setQuizId(status.getQuizId());
            dto.setQuizStatus(status.getStatus());
            dto.setQuizLatestScore(status.getLatestScore());
            return;
        }

        if (module.getQuizId() == null) {
            dto.setHasQuiz(false);
            return;
        }

        Quiz quiz = quizRepository.findById(module.getQuizId()).orElse(null);
        if (quiz == null) {
            dto.setHasQuiz(false);
            return;
        }

        dto.setHasQuiz(true);
        dto.setQuizId(quiz.getId());
        dto.setQuiz(studentQuizService.buildStatus(quiz.getId(), quiz.getTitle(), "module", module.getId(), null, studentId));

        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentIdAndQuizId(studentId, quiz.getId());
        if (attempts == null || attempts.isEmpty()) {
            dto.setQuizStatus("disponibil");
            return;
        }

        QuizAttempt latest = quizAttemptRepository
                .findTopByStudentIdAndQuizIdOrderByAttemptedAtDesc(studentId, quiz.getId())
                .orElse(null);
        dto.setQuizLatestScore(latest != null ? latest.getScore() : null);

        boolean hasPassed = attempts.stream().anyMatch(QuizAttempt::isPassed);
        dto.setQuizStatus(hasPassed ? "promovat" : "picat");
    }

    private Lecture findLectureInCourse(Course course, String lectureId) {
        if (course.getModules() == null) return null;
        for (CourseModule module : course.getModules()) {
            if (module.getLectures() == null) continue;
            for (Lecture lecture : module.getLectures()) {
                if (lectureId.equals(lecture.getId())) return lecture;
            }
        }
        return null;
    }

    private String resolveLectureType(Lecture lecture) {
        if (lecture.getType() != null && !lecture.getType().isBlank()) return lecture.getType();
        String content = resolveLectureContent(lecture).toLowerCase(Locale.ROOT);
        if (lecture.getPdfUrl() != null || content.endsWith(".pdf")) return "pdf";
        if (content.endsWith(".md") || content.endsWith(".markdown") || (!content.startsWith("http") && !content.isBlank())) {
            return "markdown";
        }
        return "video";
    }

    private String resolveLectureContent(Lecture lecture) {
        if (lecture.getContent() != null) return lecture.getContent();
        if (lecture.getPdfUrl() != null) return lecture.getPdfUrl();
        return lecture.getVideoUrl() != null ? lecture.getVideoUrl() : "";
    }

    private String resolvePdfUrl(Lecture lecture) {
        if (lecture.getPdfUrl() != null) return lecture.getPdfUrl();
        return "pdf".equals(resolveLectureType(lecture)) ? resolveLectureContent(lecture) : null;
    }

    private void updateLectureDurationIfNeeded(String courseId, Course course, Lecture lecture, Integer durationSecs) {
        if (durationSecs == null || durationSecs <= 0) return;
        if (lecture.getDurationSecs() > 0 && Math.abs(lecture.getDurationSecs() - durationSecs) <= 1) return;

        CourseModule module = null;
        if (course.getModules() != null) {
            for (CourseModule candidate : course.getModules()) {
                if (candidate.getLectures() == null) continue;
                boolean containsLecture = candidate.getLectures().stream()
                        .anyMatch(l -> lecture.getId().equals(l.getId()));
                if (containsLecture) {
                    module = candidate;
                    break;
                }
            }
        }
        if (module == null || module.getLectures() == null) return;

        for (Lecture item : module.getLectures()) {
            if (lecture.getId().equals(item.getId())) {
                item.setDurationSecs(durationSecs);
                lecture.setDurationSecs(durationSecs);
                break;
            }
        }

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId).and("modules.id").is(module.getId())),
                new Update().set("modules.$", module).set("updatedAt", new Date()),
                Course.class
        );
    }
}
