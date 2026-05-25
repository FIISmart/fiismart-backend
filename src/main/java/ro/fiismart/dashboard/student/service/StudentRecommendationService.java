package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.dashboard.student.dto.RecommendationDTO;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class StudentRecommendationService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    public StudentRecommendationService(EnrollmentRepository enrollmentRepository,
                                        CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
    }

    public RecommendationDTO getRecommendation(String studentId) {
        Set<String> enrolledIds = enrollmentRepository.findByStudentId(studentId)
                .stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toSet());

        List<Course> notEnrolled = courseRepository.findAll().stream()
                .filter(c -> !enrolledIds.contains(c.getId()))
                .collect(Collectors.toList());

        if (notEnrolled.isEmpty()) return null;

        Course recommended = notEnrolled.get(ThreadLocalRandom.current().nextInt(notEnrolled.size()));

        RecommendationDTO dto = new RecommendationDTO();
        dto.setCourseId(recommended.getId());
        dto.setTitle(recommended.getTitle());
        dto.setDescription(recommended.getDescription());
        dto.setThumbnailUrl(recommended.getThumbnailUrl());
        dto.setAvgRating(recommended.getAvgRating());
        dto.setEnrollmentCount(recommended.getEnrollmentCount());
        return dto;
    }
}
