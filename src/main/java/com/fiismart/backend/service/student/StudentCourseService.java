package com.fiismart.backend.service.student;

import com.fiismart.backend.dto.student.StudentCourseHeaderDTO;
import database.dao.CourseDAO;
import database.dao.EnrollmentDAO;
import database.dao.UserDAO;
import database.model.Course;
import database.model.Enrollment;
import database.model.User;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Service
public class StudentCourseService {

    private final CourseDAO courseDAO = new CourseDAO();
    private final UserDAO userDAO = new UserDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    public StudentCourseHeaderDTO getHeader(String studentIdHex, String courseIdHex) {
        ObjectId studentId = new ObjectId(studentIdHex);
        ObjectId courseId = new ObjectId(courseIdHex);

        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new RuntimeException("Course not found: " + courseIdHex);
        }

        User teacher = course.getTeacherId() != null
                ? userDAO.findById(course.getTeacherId())
                : null;

        Enrollment enrollment = enrollmentDAO.findByStudentAndCourse(studentId, courseId);

        StudentCourseHeaderDTO dto = new StudentCourseHeaderDTO();
        dto.setCourseId(course.getId().toHexString());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setThumbnailUrl(course.getThumbnailUrl());
        dto.setLanguage(course.getLanguage());
        dto.setStatus(course.getStatus());
        dto.setTags(course.getTags());
        dto.setTeacherId(course.getTeacherId() != null ? course.getTeacherId().toHexString() : null);
        dto.setTeacherDisplayName(teacher != null ? teacher.getDisplayName() : "");
        dto.setAvgRating(course.getAvgRating());
        dto.setEnrollmentCount(course.getEnrollmentCount());
        dto.setEnrolled(enrollment != null);
        dto.setOverallProgress(enrollment != null ? enrollment.getOverallProgress() : 0);

        return dto;
    }
}