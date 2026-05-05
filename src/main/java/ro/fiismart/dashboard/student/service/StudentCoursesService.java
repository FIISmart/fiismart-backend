package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.dashboard.student.dto.CourseSummaryDTO;

import java.util.ArrayList;
import java.util.List;

@Service
public class StudentCoursesService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    public StudentCoursesService(EnrollmentRepository enrollmentRepository,
                                 CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
    }

    public List<CourseSummaryDTO> getCourses(String studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<CourseSummaryDTO> result = new ArrayList<>();

        for (Enrollment e : enrollments) {
            Course course = courseRepository.findById(e.getCourseId()).orElse(null);
            if (course == null) continue;

            CourseSummaryDTO dto = new CourseSummaryDTO();
            dto.setCourseId(course.getId());
            dto.setTitle(course.getTitle());
            dto.setDescription(course.getDescription());
            dto.setThumbnailUrl(course.getThumbnailUrl());
            dto.setAvgRating(course.getAvgRating());
            dto.setEnrollmentCount(course.getEnrollmentCount());
            dto.setOverallProgress(e.getOverallProgress());
            dto.setStatus(e.getStatus());
            result.add(dto);
        }
        return result;
    }
}
