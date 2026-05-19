package ro.fiismart.enrollment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.LectureProgressEntry;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.enrollment.dto.EnrollmentRequest;
import ro.fiismart.enrollment.dto.EnrollmentResponse;
import ro.fiismart.notification.service.NotificationService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final MongoTemplate mongoTemplate;
    private final NotificationService notificationService;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public EnrollmentResponse create(EnrollmentRequest request) {
        Enrollment enrollment = Enrollment.builder()
                .studentId(request.getStudentId())
                .courseId(request.getCourseId())
                .enrolledAt(new Date())
                .status("enrolled")
                .overallProgress(0)
                .lectureProgress(new ArrayList<>())
                .build();
        EnrollmentResponse response = toResponse(enrollmentRepository.save(enrollment));

        // Notifică profesorul și studentul
        try {
            courseRepository.findById(request.getCourseId()).ifPresent(course -> {
                String studentName = userRepository.findById(request.getStudentId())
                        .map(u -> u.getDisplayName() != null ? u.getDisplayName() : "Un student")
                        .orElse("Un student");
                notificationService.createEnrollmentNotification(
                        course.getTeacherId(), studentName, course.getTitle(), course.getId());
                notificationService.createStudentEnrollmentNotification(
                        request.getStudentId(), course.getTitle(), course.getId());
            });
        } catch (Exception ignored) {
            // Nu eșuăm înrolarea din cauza notificării
        }

        return response;
    }

    public EnrollmentResponse findById(String id) {
        return toResponse(enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + id)));
    }

    public EnrollmentResponse findByStudentAndCourse(String studentId, String courseId) {
        return toResponse(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found for student " + studentId + " and course " + courseId)));
    }

    public List<EnrollmentResponse> findByStudentId(String studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream().map(this::toResponse).toList();
    }

    public List<EnrollmentResponse> findByCourseId(String courseId) {
        return enrollmentRepository.findByCourseId(courseId).stream().map(this::toResponse).toList();
    }

    public List<EnrollmentResponse> findCompletedByStudent(String studentId) {
        return enrollmentRepository.findByStudentIdAndStatus(studentId, "completed")
                .stream().map(this::toResponse).toList();
    }

    public void updateStatus(String enrollmentId, String status) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(enrollmentId)),
                new Update().set("status", status),
                Enrollment.class);
    }

    public void updateOverallProgress(String enrollmentId, int progress) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(enrollmentId)),
                new Update().set("overallProgress", progress),
                Enrollment.class);
    }

    public void markCompleted(String enrollmentId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(enrollmentId)),
                new Update()
                        .set("status", "completed")
                        .set("completedAt", new Date())
                        .set("overallProgress", 100),
                Enrollment.class);
    }

    public void addLectureProgress(String enrollmentId, LectureProgressEntry entry) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(enrollmentId)),
                new Update().push("lectureProgress", entry),
                Enrollment.class);
    }

    public void deleteById(String enrollmentId) {
        enrollmentRepository.deleteById(enrollmentId);
    }

    public void deleteByStudentAndCourse(String studentId, String courseId) {
        enrollmentRepository.deleteByStudentIdAndCourseId(studentId, courseId);
    }

    public boolean isEnrolled(String studentId, String courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    public long countByCourse(String courseId) {
        return enrollmentRepository.countByCourseId(courseId);
    }

    private EnrollmentResponse toResponse(Enrollment e) {
        return EnrollmentResponse.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .courseId(e.getCourseId())
                .enrolledAt(e.getEnrolledAt())
                .completedAt(e.getCompletedAt())
                .status(e.getStatus())
                .lectureProgress(e.getLectureProgress())
                .lastAccessedAt(e.getLastAccessedAt())
                .overallProgress(e.getOverallProgress())
                .build();
    }
}
