package com.fiismart.teacher_dashboard.service;

import com.fiismart.teacher_dashboard.dto.TeacherCourseDTO;
import database.dao.CourseDAO;
import database.dao.EnrollmentDAO;
import database.model.Course;
import database.model.Enrollment;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherCoursesService {
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    public List<TeacherCourseDTO> getCourses(String teacherIdHex, String status, int limit, int offset) {
        ObjectId teacherId = new ObjectId(teacherIdHex);

        List<Course> courses = courseDAO.findByTeacherId(teacherId);

        if( status != null && !status.equals("all")) {
            courses = courses.stream()
                    .filter(c -> status.equalsIgnoreCase(c.getStatus()))
                    .collect(Collectors.toList());
        }

        courses.sort(Comparator.comparing(Course::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        courses = courses.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        List<TeacherCourseDTO> result = new ArrayList<>();
        for( Course course : courses) {
            TeacherCourseDTO dto = new TeacherCourseDTO();
            dto.setCourseId(course.getId().toHexString());
            dto.setTitle(course.getTitle());
            dto.setDescription(course.getDescription());
            dto.setStatus(course.getStatus());
            dto.setEnrollmentCount(course.getEnrollmentCount());
            dto.setAvgRating(course.getAvgRating());
            dto.setUpdatedAt(course.getUpdatedAt());
            result.add(dto);
        }

        return result;

    }
}
