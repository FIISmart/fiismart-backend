package ro.fiismart.landing;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/landing")
@RequiredArgsConstructor
public class LandingController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        List<Course> publishedCourses = publishedCourses();
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeStudents", userRepository.countByRole("student"));
        stats.put("totalTeachers", userRepository.countByRole("professor"));
        stats.put("freeCourses", publishedCourses.size());
        double avgRating = publishedCourses.stream()
                .mapToDouble(Course::getAvgRating)
                .filter(rating -> rating > 0)
                .average()
                .orElse(0);
        stats.put("satisfactionRate", avgRating > 0 ? String.format(Locale.US, "%.1f/5", avgRating) : "0/5");
        return stats;
    }

    @GetMapping("/categories")
    public Map<String, Long> getCategories() {
        List<Course> courses = publishedCourses();
        Map<String, Long> categories = courses.stream()
                .filter(c -> c.getTags() != null && !c.getTags().isEmpty())
                .collect(Collectors.groupingBy(
                        c -> c.getTags().get(0),
                        Collectors.counting()
                ));
        categories.put("Toate", (long) courses.size());
        return categories;
    }

    @GetMapping("/courses/popular")
    public List<Map<String, Object>> getPopularCourses() {
        return publishedCourses().stream()
                .limit(6)
                .map(c -> {
                    Map<String, Object> card = new HashMap<>();
                    card.put("id", c.getId());
                    card.put("title", c.getTitle() != null ? c.getTitle() : "Curs fara titlu");
                    card.put("description", c.getDescription());
                    card.put("thumbnailUrl", c.getThumbnailUrl());
                    card.put("avgRating", c.getAvgRating());
                    card.put("enrollmentCount", c.getEnrollmentCount());
                    card.put("tags", c.getTags());
                    card.put("teacherName", userRepository.findById(c.getTeacherId())
                            .map(u -> u.getDisplayName() != null && !u.getDisplayName().isBlank()
                                    ? u.getDisplayName()
                                    : "Profesor FII Smart")
                            .orElse("Profesor FII Smart"));
                    int durationSecs = c.getModules() == null ? 0 : c.getModules().stream()
                            .filter(Objects::nonNull)
                            .flatMap(module -> module.getLectures() == null
                                    ? java.util.stream.Stream.empty()
                                    : module.getLectures().stream())
                            .filter(Objects::nonNull)
                            .mapToInt(lecture -> Math.max(0, lecture.getDurationSecs()))
                            .sum();
                    card.put("durationSecs", durationSecs);
                    return card;
                })
                .collect(Collectors.toList());
    }

    private List<Course> publishedCourses() {
        return courseRepository.findAll().stream()
                .filter(c -> "published".equalsIgnoreCase(c.getStatus()) && !c.isHidden())
                .toList();
    }
}
