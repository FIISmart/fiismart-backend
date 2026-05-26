package ro.fiismart.preview;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.exception.ForbiddenException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Professor "preview as student" endpoint. Lets a professor view their own
 * course in the LessonVideoPage shell without going through enrollment. The
 * FE calls this on the /professor/preview/:courseId/* route family.
 *
 * Returns a payload with the same fields the LessonVideoPage consumes from
 * the student getCourseInfo endpoint (title, description, teacher info,
 * tags, enrollmentCount, modules[]). Modules are returned verbatim from
 * the Course document so the FE can resolve lecture-by-id and navigate
 * between lectures.
 */
@RestController
@RequestMapping("/api/v1/preview")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSOR')")
public class PreviewController {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @GetMapping("/courses/{courseId}")
    public Map<String, Object> previewCourse(@PathVariable String courseId,
                                              @AuthenticationPrincipal String userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        // Only the course's own teacher can preview it. Admin override is
        // intentionally NOT added here — admins should use a separate
        // /admin/* path if they need view-as-student behaviour.
        if (!userId.equals(course.getTeacherId())) {
            throw new ForbiddenException("Not your course");
        }

        User teacher = course.getTeacherId() != null
                ? userRepository.findById(course.getTeacherId()).orElse(null)
                : null;

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("courseId", course.getId());
        dto.put("title", course.getTitle());
        dto.put("description", course.getDescription());
        dto.put("thumbnailUrl", course.getThumbnailUrl());
        dto.put("language", course.getLanguage());
        dto.put("status", course.getStatus());
        dto.put("tags", course.getTags());
        dto.put("teacherId", course.getTeacherId());
        dto.put("teacherDisplayName", teacher != null ? teacher.getDisplayName() : "");
        dto.put("avgRating", course.getAvgRating());
        dto.put("enrollmentCount", course.getEnrollmentCount());
        // Preview is owner-only — they're effectively "enrolled" for UX purposes.
        dto.put("enrolled", true);
        dto.put("overallProgress", 0);
        // Modules + embedded lectures verbatim so the FE can pick the
        // first lecture / navigate next/prev.
        dto.put("modules", course.getModules());
        return dto;
    }
}
