package com.fiismart.backend.service.student;

import com.fiismart.backend.dto.student.StudentLectureDTO;
import com.fiismart.backend.dto.student.StudentLectureDetailDTO;
import com.fiismart.backend.dto.student.StudentLectureProgressRequest;
import com.fiismart.backend.dto.student.StudentLectureProgressResponse;
import com.fiismart.backend.dto.student.StudentModuleDTO;
import database.dao.CourseDAO;
import database.dao.EnrollmentDAO;
import database.dao.QuizAttemptDAO;
import database.dao.QuizDAO;
import database.model.Course;
import database.model.CourseModule;
import database.model.Enrollment;
import database.model.Lecture;
import database.model.Quiz;
import database.model.QuizAttempt;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentLectureService {

    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizAttemptDAO quizAttemptDAO = new QuizAttemptDAO();

    public List<StudentModuleDTO> getModules(String studentIdHex, String courseIdHex) {
        ObjectId studentId = new ObjectId(studentIdHex);
        ObjectId courseId = new ObjectId(courseIdHex);

        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new RuntimeException("Course not found: " + courseIdHex);
        }

        List<CourseModule> modules = course.getModules() != null
                ? course.getModules()
                : new ArrayList<>();

        Enrollment enrollment = enrollmentDAO.findByStudentAndCourse(studentId, courseId);

        Map<ObjectId, Document> progressByLectureId = buildProgressMap(enrollment);

        List<StudentModuleDTO> result = new ArrayList<>();
        for (CourseModule module : modules) {
            StudentModuleDTO dto = new StudentModuleDTO();
            dto.setModuleId(module.getId().toHexString());
            dto.setTitle(module.getTitle());
            dto.setDescription(module.getDescription());
            dto.setOrder(module.getOrder());

            List<StudentLectureDTO> lectureDtos = new ArrayList<>();
            List<Lecture> lectures = module.getLectures() != null
                    ? module.getLectures()
                    : new ArrayList<>();
            for (Lecture lecture : lectures) {
                StudentLectureDTO lectureDto = buildLectureDTO(lecture, progressByLectureId.get(lecture.getId()));
                lectureDtos.add(lectureDto);
            }
            lectureDtos.sort(Comparator.comparingInt(StudentLectureDTO::getOrder));
            dto.setLectures(lectureDtos);

            populateModuleQuizInfo(dto, module, studentId);

            result.add(dto);
        }

        result.sort(Comparator.comparingInt(StudentModuleDTO::getOrder));

        return result;
    }

    public StudentLectureDetailDTO getLectureDetail(String studentIdHex, String courseIdHex, String lectureIdHex) {
        ObjectId studentId = new ObjectId(studentIdHex);
        ObjectId courseId = new ObjectId(courseIdHex);
        ObjectId lectureId = new ObjectId(lectureIdHex);

        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new RuntimeException("Course not found: " + courseIdHex);
        }

        Lecture lecture = findLectureInCourse(course, lectureId);
        if (lecture == null) {
            throw new RuntimeException("Lecture not found in course: " + lectureIdHex);
        }

        Enrollment enrollment = enrollmentDAO.findByStudentAndCourse(studentId, courseId);

        Document progress = null;
        if (enrollment != null && enrollment.getLectureProgress() != null) {
            for (Document p : enrollment.getLectureProgress()) {
                if (lectureId.equals(p.getObjectId("lectureId"))) {
                    progress = p;
                    break;
                }
            }
        }

        StudentLectureDetailDTO dto = new StudentLectureDetailDTO();
        dto.setLectureId(lecture.getId().toHexString());
        dto.setTitle(lecture.getTitle());
        dto.setVideoUrl(lecture.getVideoUrl());
        dto.setImageUrls(lecture.getImageUrls());
        dto.setOrder(lecture.getOrder());
        dto.setDurationSecs(lecture.getDurationSecs());
        dto.setPublishedAt(lecture.getPublishedAt());

        if (progress != null) {
            dto.setWatchedPercent(progress.getInteger("watchedPercent", 0));
            dto.setPositionSecs(progress.getInteger("positionSecs", 0));
            dto.setCompleted(Boolean.TRUE.equals(progress.getBoolean("completed")));
        } else {
            dto.setWatchedPercent(0);
            dto.setPositionSecs(0);
            dto.setCompleted(false);
        }

        return dto;
    }

    public StudentLectureProgressResponse updateLectureProgress(
            String studentIdHex,
            String courseIdHex,
            String lectureIdHex,
            StudentLectureProgressRequest request) {

        ObjectId studentId = new ObjectId(studentIdHex);
        ObjectId courseId = new ObjectId(courseIdHex);
        ObjectId lectureId = new ObjectId(lectureIdHex);

        // 1. Validăm că cursul și lectura există
        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new RuntimeException("Course not found: " + courseIdHex);
        }

        Lecture lecture = findLectureInCourse(course, lectureId);
        if (lecture == null) {
            throw new RuntimeException("Lecture not found in course: " + lectureIdHex);
        }

        // 2. Validăm că studentul e înscris la curs
        Enrollment enrollment = enrollmentDAO.findByStudentAndCourse(studentId, courseId);
        if (enrollment == null) {
            throw new RuntimeException("Student is not enrolled in course: " + courseIdHex);
        }

        // 3. Clamp watchedPercent în [0, 100]
        int watchedPercent = Math.max(0, Math.min(100, request.getWatchedPercent()));

        // 4. Validăm positionSecs — nu poate fi negativ, nu poate depăși durata
        int positionSecs = Math.max(0, request.getPositionSecs());
        if (lecture.getDurationSecs() > 0 && positionSecs > lecture.getDurationSecs()) {
            positionSecs = lecture.getDurationSecs();
        }

        // 5. Backend forțează regula: completed == (watchedPercent == 100)
        boolean completed = (watchedPercent == 100);

        // 6. Construim documentul de progres (include moduleId)
        Date now = new Date();
        Document progressDoc = new Document()
                .append("lectureId", lectureId)
                .append("moduleId", lecture.getModuleId())
                .append("watchedPercent", watchedPercent)
                .append("positionSecs", positionSecs)
                .append("completed", completed)
                .append("updatedAt", now);

        // 7. Upsert în lectureProgress
        enrollmentDAO.upsertLectureProgress(enrollment.getId(), lectureId, progressDoc);

        // 8. Recalculăm overallProgress
        int overallProgress = recalculateOverallProgress(course, enrollment.getId(), lectureId, completed);

        // 9. Actualizăm overallProgress pe enrollment
        enrollmentDAO.updateOverallProgress(enrollment.getId(), overallProgress);

        // 10. Actualizăm lastAccessedAt
        enrollmentDAO.updateLastAccessedAt(enrollment.getId(), now);

        // 11. Dacă 100% → marcăm enrollment ca completed
        String enrollmentStatus = enrollment.getStatus();
        boolean courseCompleted = false;
        if (overallProgress >= 100) {
            enrollmentDAO.markCompleted(enrollment.getId(), now);
            enrollmentStatus = "completed";
            courseCompleted = true;
        }

        // 12. Construim răspunsul
        StudentLectureProgressResponse response = new StudentLectureProgressResponse();
        response.setLectureId(lectureId.toHexString());
        response.setWatchedPercent(watchedPercent);
        response.setPositionSecs(positionSecs);
        response.setCompleted(completed);
        response.setOverallProgress(overallProgress);
        response.setEnrollmentStatus(enrollmentStatus);
        response.setCourseCompleted(courseCompleted);

        return response;
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private Map<ObjectId, Document> buildProgressMap(Enrollment enrollment) {
        Map<ObjectId, Document> map = new HashMap<>();
        if (enrollment != null && enrollment.getLectureProgress() != null) {
            for (Document progress : enrollment.getLectureProgress()) {
                ObjectId lectureId = progress.getObjectId("lectureId");
                if (lectureId != null) {
                    map.put(lectureId, progress);
                }
            }
        }
        return map;
    }

    private StudentLectureDTO buildLectureDTO(Lecture lecture, Document progress) {
        StudentLectureDTO dto = new StudentLectureDTO();
        dto.setLectureId(lecture.getId().toHexString());
        dto.setTitle(lecture.getTitle());
        dto.setOrder(lecture.getOrder());
        dto.setDurationSecs(lecture.getDurationSecs());

        if (progress != null) {
            dto.setWatchedPercent(progress.getInteger("watchedPercent", 0));
            dto.setPositionSecs(progress.getInteger("positionSecs", 0));
            dto.setCompleted(Boolean.TRUE.equals(progress.getBoolean("completed")));
        } else {
            dto.setWatchedPercent(0);
            dto.setPositionSecs(0);
            dto.setCompleted(false);
        }
        return dto;
    }

    private void populateModuleQuizInfo(StudentModuleDTO dto, CourseModule module, ObjectId studentId) {
        if (module.getQuizId() == null) {
            dto.setHasQuiz(false);
            dto.setQuizId(null);
            dto.setQuizStatus(null);
            dto.setQuizLatestScore(null);
            return;
        }

        Quiz quiz = quizDAO.findById(module.getQuizId());
        if (quiz == null) {
            dto.setHasQuiz(false);
            dto.setQuizId(null);
            dto.setQuizStatus(null);
            dto.setQuizLatestScore(null);
            return;
        }

        dto.setHasQuiz(true);
        dto.setQuizId(quiz.getId().toHexString());

        List<QuizAttempt> attempts = quizAttemptDAO.findByStudentAndQuiz(studentId, quiz.getId());
        if (attempts == null || attempts.isEmpty()) {
            dto.setQuizStatus("disponibil");
            dto.setQuizLatestScore(null);
            return;
        }

        QuizAttempt latest = quizAttemptDAO.findLatestAttempt(studentId, quiz.getId());
        dto.setQuizLatestScore(latest != null ? latest.getScore() : null);

        boolean hasPassed = attempts.stream().anyMatch(QuizAttempt::isPassed);
        dto.setQuizStatus(hasPassed ? "promovat" : "picat");
    }

    private Lecture findLectureInCourse(Course course, ObjectId lectureId) {
        if (course.getModules() == null) return null;
        for (CourseModule module : course.getModules()) {
            if (module.getLectures() == null) continue;
            for (Lecture lecture : module.getLectures()) {
                if (lectureId.equals(lecture.getId())) {
                    return lecture;
                }
            }
        }
        return null;
    }

    /**
     * Recalculează overallProgress pe baza: lecturi completate / total lecturi * 100.
     *
     * Pentru lectura curentă (care tocmai a fost updatată), folosim `currentLectureCompleted`
     * primit ca parametru, pentru că enrollment-ul în memorie încă n-are modificarea.
     * Pentru restul lecturilor, citim din enrollment-ul din DB.
     */
    private int recalculateOverallProgress(Course course, ObjectId enrollmentId, ObjectId currentLectureId, boolean currentLectureCompleted) {
        // Reîncărcăm enrollment-ul pentru a avea datele proaspete (după upsert)
        Enrollment refreshedEnrollment = enrollmentDAO.findById(enrollmentId);
        Map<ObjectId, Document> progressByLectureId = buildProgressMap(refreshedEnrollment);

        int totalLectures = 0;
        int completedLectures = 0;

        if (course.getModules() != null) {
            for (CourseModule module : course.getModules()) {
                if (module.getLectures() == null) continue;
                for (Lecture lecture : module.getLectures()) {
                    totalLectures++;
                    Document progress = progressByLectureId.get(lecture.getId());
                    if (progress != null && Boolean.TRUE.equals(progress.getBoolean("completed"))) {
                        completedLectures++;
                    }
                }
            }
        }

        if (totalLectures == 0) return 0;
        return (int) Math.round((double) completedLectures / totalLectures * 100);
    }
}