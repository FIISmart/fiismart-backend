package ro.fiismart.dashboard.student.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.model.*;
import ro.fiismart.common.repository.*;
import ro.fiismart.dashboard.student.dto.*;
import java.util.Optional;

import java.util.*;

@Service
public class StudentLectureService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final ModuleQuizRepository moduleQuizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final MongoTemplate mongoTemplate;

    public StudentLectureService(CourseRepository courseRepository,
                                 EnrollmentRepository enrollmentRepository,
                                 QuizRepository quizRepository,
                                 ModuleQuizRepository moduleQuizRepository,
                                 QuizAttemptRepository quizAttemptRepository,
                                 MongoTemplate mongoTemplate) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.moduleQuizRepository = moduleQuizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<StudentModuleDTO> getModules(String studentId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

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
                lectureDtos.add(buildLectureDTO(lecture, progress));
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
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        Lecture lecture = findLectureInCourse(course, lectureId);
        if (lecture == null) throw new RuntimeException("Lecture not found: " + lectureId);

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).orElse(null);
        LectureProgressEntry progress = findLatestProgress(enrollment, lectureId);

        StudentLectureDetailDTO dto = new StudentLectureDetailDTO();
        dto.setLectureId(lecture.getId());
        dto.setTitle(lecture.getTitle());
        dto.setVideoUrl(lecture.getVideoUrl());
        dto.setImageUrls(lecture.getImageUrls());
        dto.setOrder(lecture.getOrder());
        dto.setDurationSecs(lecture.getDurationSecs());
        dto.setPublishedAt(lecture.getPublishedAt());

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
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        Lecture lecture = findLectureInCourse(course, lectureId);
        if (lecture == null) throw new RuntimeException("Lecture not found: " + lectureId);

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new RuntimeException("Student not enrolled: " + courseId));

        int watchedPercent = request.getWatchedPercent();
        if (watchedPercent < 0) watchedPercent = 0;
        if (watchedPercent > 100) watchedPercent = 100;

        int positionSecs = Math.max(0, request.getPositionSecs());
        if (lecture.getDurationSecs() > 0 && positionSecs > lecture.getDurationSecs()) {
            positionSecs = lecture.getDurationSecs();
        }

        LectureProgressEntry existingProgress = findLatestProgress(enrollment, lectureId);

        boolean completed = (watchedPercent >= 95);

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


    private StudentLectureDTO buildLectureDTO(Lecture lecture, LectureProgressEntry progress) {
        StudentLectureDTO dto = new StudentLectureDTO();
        dto.setLectureId(lecture.getId());
        dto.setTitle(lecture.getTitle());
        dto.setOrder(lecture.getOrder());
        dto.setDurationSecs(lecture.getDurationSecs());
        if (progress != null) {
            dto.setWatchedPercent(progress.getWatchedPercent());
            dto.setPositionSecs(progress.getPositionSecs());
            dto.setCompleted(progress.isCompleted());
        }
        return dto;
    }

    private void populateModuleQuizInfo(StudentModuleDTO dto, CourseModule module, String studentId) {
        // Try the new ModuleQuiz collection first (Course Builder quizzes)
        Optional<ModuleQuiz> moduleQuizOpt = moduleQuizRepository.findByModuleIdAndQuizScope(module.getId(), "module");

        String resolvedQuizId = null;
        if (moduleQuizOpt.isPresent()) {
            resolvedQuizId = moduleQuizOpt.get().getId();
        } else if (module.getQuizId() != null) {
            Quiz legacy = quizRepository.findById(module.getQuizId()).orElse(null);
            if (legacy != null) resolvedQuizId = legacy.getId();
        }

        if (resolvedQuizId == null) {
            dto.setQuiz(null);
            return;
        }

        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentIdAndQuizId(studentId, resolvedQuizId);
        boolean hasPassed = attempts != null && attempts.stream().anyMatch(QuizAttempt::isPassed);
        int lastScore = 0;
        if (attempts != null && !attempts.isEmpty()) {
            QuizAttempt latest = quizAttemptRepository
                    .findTopByStudentIdAndQuizIdOrderByAttemptedAtDesc(studentId, resolvedQuizId)
                    .orElse(null);
            lastScore = latest != null ? latest.getScore() : 0;
        }

        String statusLabel = (attempts == null || attempts.isEmpty()) ? "Disponibil"
                : hasPassed ? "Promovat" : "Picat";

        StudentModuleDTO.QuizInfo quizInfo = new StudentModuleDTO.QuizInfo();
        quizInfo.setQuizId(resolvedQuizId);
        quizInfo.setAttemptCount(attempts != null ? attempts.size() : 0);
        quizInfo.setLastScore(lastScore);
        quizInfo.setPassed(hasPassed);
        quizInfo.setStatusLabel(statusLabel);
        dto.setQuiz(quizInfo);
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
}
