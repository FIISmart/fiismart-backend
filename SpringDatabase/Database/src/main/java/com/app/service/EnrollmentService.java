package com.app.service;

import com.app.dto.request.EnrollmentRequest;
import com.app.dto.response.EnrollmentResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Enrollment;
import com.app.model.LectureProgress;
import com.app.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final MongoTemplate mongoTemplate;

    public EnrollmentResponse create(EnrollmentRequest request) {
        Enrollment enrollment = Enrollment.builder()
                .studentId(request.getStudentId())
                .courseId(request.getCourseId())
                .enrolledAt(new Date())
                .status("enrolled")
                .overallProgress(0)
                .lectureProgress(new ArrayList<>())
                .build();
        return toResponse(enrollmentRepository.save(enrollment));
    }

    public EnrollmentResponse findById(String id) {
        return toResponse(enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id)));
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

    public void updateLastAccessedAt(String enrollmentId, Date lastAccessedAt) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(enrollmentId)),
                new Update().set("lastAccessedAt", lastAccessedAt),
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

    public void addLectureProgress(String enrollmentId, LectureProgress lectureProgress) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(enrollmentId)),
                new Update().push("lectureProgress", lectureProgress),
                Enrollment.class);
    }

    public void updateLectureProgressField(String enrollmentId, String lectureId, String field, Object value) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(enrollmentId).and("lectureProgress.lectureId").is(lectureId)),
                new Update().set("lectureProgress.$." + field, value),
                Enrollment.class);
    }

    public void removeLectureProgress(String enrollmentId, String lectureId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(enrollmentId)),
                new Update().pull("lectureProgress", new org.bson.Document("lectureId", lectureId)),
                Enrollment.class);
    }

    public void deleteById(String enrollmentId) {
        enrollmentRepository.deleteById(enrollmentId);
    }

    public void deleteByStudentAndCourse(String studentId, String courseId) {
        enrollmentRepository.deleteByStudentIdAndCourseId(studentId, courseId);
    }

    public void deleteAllByCourse(String courseId) {
        enrollmentRepository.deleteByCourseId(courseId);
    }

    public boolean isEnrolled(String studentId, String courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    public long countByCourse(String courseId) {
        return enrollmentRepository.countByCourseId(courseId);
    }

    public long countCompletedByCourse(String courseId) {
        return enrollmentRepository.countByCourseIdAndStatus(courseId, "completed");
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
