package ro.fiismart.dashboard.teacher.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.dashboard.teacher.dto.TeacherCoursesDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherCoursesService {

    private final CourseRepository courseRepository;

    public TeacherCoursesService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<TeacherCoursesDTO> getCourses(String teacherId, String status, int limit, int offset) {
        List<Course> courses = courseRepository.findByTeacherId(teacherId);

        if (status != null && !status.equals("all")) {
            courses = courses.stream()
                    .filter(c -> status.equalsIgnoreCase(c.getStatus()))
                    .collect(Collectors.toList());
        }

        courses.sort(Comparator.comparing(Course::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        courses = courses.stream().skip(offset).limit(limit).collect(Collectors.toList());

        List<TeacherCoursesDTO> result = new ArrayList<>();
        for (Course course : courses) {
            TeacherCoursesDTO dto = new TeacherCoursesDTO();
            dto.setCourseId(course.getId());
            dto.setTitle(course.getTitle());
            dto.setDescription(course.getDescription());
            dto.setStatus(course.getStatus());
            dto.setEnrollmentCount(course.getEnrollmentCount());
            dto.setAvgRating(course.getAvgRating());
            dto.setUpdatedAt(course.getUpdatedAt());
            dto.setThumbnailUrl(course.getThumbnailUrl());
            result.add(dto);
        }
        return result;
    }
}
