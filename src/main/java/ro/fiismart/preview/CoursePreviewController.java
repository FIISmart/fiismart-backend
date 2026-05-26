package ro.fiismart.preview;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.dashboard.student.dto.StudentCourseHeaderDTO;

@RestController
@RequestMapping("/api/v1/preview")
public class CoursePreviewController {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CoursePreviewController(CourseRepository courseRepository,
                                   UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/courses/{courseId}")
    public StudentCourseHeaderDTO getCoursePreview(
            @PathVariable String courseId,
            @AuthenticationPrincipal Jwt jwt) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        User teacher = course.getTeacherId() != null
                ? userRepository.findById(course.getTeacherId()).orElse(null)
                : null;

        StudentCourseHeaderDTO dto = new StudentCourseHeaderDTO();
        dto.setCourseId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setThumbnailUrl(course.getThumbnailUrl());
        dto.setLanguage(course.getLanguage());
        dto.setStatus(course.getStatus());
        dto.setTags(course.getTags());
        dto.setTeacherId(course.getTeacherId());
        dto.setTeacherDisplayName(teacher != null ? teacher.getDisplayName() : "");
        dto.setAvgRating(course.getAvgRating());
        dto.setEnrollmentCount(course.getEnrollmentCount());
        dto.setEnrolled(false);
        dto.setOverallProgress(0);
        dto.setFinalQuiz(null);
        return dto;
    }
}