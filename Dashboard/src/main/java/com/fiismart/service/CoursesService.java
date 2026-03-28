package com.fiismart.service;

import com.fiismart.dto.CourseSummaryDTO;
import database.dao.CourseDAO;
import database.dao.EnrollmentDAO;
import database.model.Course;
import database.model.Enrollment;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CoursesService {

    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final CourseDAO courseDAO = new CourseDAO();

    public List<CourseSummaryDTO> getCourses(String studentId) {
        ObjectId id = new ObjectId(studentId);
        List<Enrollment> enrollments = enrollmentDAO.findByStudentId(id);

        List<CourseSummaryDTO> result = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Course course = courseDAO.findById(e.getCourseId());
            if (course == null) continue;

            CourseSummaryDTO dto = new CourseSummaryDTO();
            dto.setCourseId(course.getId().toHexString());
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

    @Autowired
    private CourseRepository courseRepository;

    public int calculateOverallProgress(Long courseId, Long studentId) {
        Optional<Course> courseOptional = courseRepository.findById(courseId);

        if (courseOptional.isEmpty()) {
            return 0;
        }

        Course currentCourse = courseOptional.get();
        List<Task> allCourseTasks = currentCourse.getTasks();

        if (allCourseTasks == null || allCourseTasks.isEmpty()) {
            return 0;
        }

        long completedTasksCount = allCourseTasks.stream()
                .filter(task -> task.isCompletedByStudent(studentId))
                .count();

        double exactProgress = ((double) completedTasksCount / allCourseTasks.size()) * 100;

        return (int) Math.round(exactProgress);
    }
}