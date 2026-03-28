package com.fiismart.student.dashboard.service;

import com.fiismart.student.dashboard.dto.RecommendationDTO;
import database.dao.CourseDAO;
import database.dao.EnrollmentDAO;
import database.model.Course;
import database.model.Enrollment;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final CourseDAO courseDAO = new CourseDAO();

    public RecommendationDTO getRecommendation(String studentId) {
        ObjectId id = new ObjectId(studentId);

        // Cursurile la care e deja înscris
        List<ObjectId> enrolledCourseIds = enrollmentDAO.findByStudentId(id)
                .stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());

        // Toate cursurile
        List<Course> allCourses = courseDAO.findAll();

        // Filtrăm cursurile la care nu e înscris
        List<Course> notEnrolled = allCourses.stream()
                .filter(c -> !enrolledCourseIds.contains(c.getId()))
                .collect(Collectors.toList());

        if (notEnrolled.isEmpty()) return null;

        // Alegem unul random
        Course recommended = notEnrolled.get(new Random().nextInt(notEnrolled.size()));

        RecommendationDTO dto = new RecommendationDTO();
        dto.setCourseId(recommended.getId().toHexString());
        dto.setTitle(recommended.getTitle());
        dto.setDescription(recommended.getDescription());
        dto.setThumbnailUrl(recommended.getThumbnailUrl());
        dto.setAvgRating(recommended.getAvgRating());
        dto.setEnrollmentCount(recommended.getEnrollmentCount());

        return dto;
    }
}
