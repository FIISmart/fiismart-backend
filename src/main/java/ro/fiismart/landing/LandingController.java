package ro.fiismart.landing;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/landing")
@RequiredArgsConstructor
public class LandingController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeStudents", userRepository.countByRole("student"));
        stats.put("totalTeachers", userRepository.countByRole("teacher"));
        long publishedCoursesCount = courseRepository.findAll().stream()
                .filter(c -> !c.isHidden())
                .count();
        stats.put("freeCourses", publishedCoursesCount);
        stats.put("satisfactionRate", "98%");
        return stats;
    }

    @GetMapping("/categories")
    public Map<String, Long> getCategories() {
        List<Course> allCourses = courseRepository.findAll();
        Map<String, Long> categories = allCourses.stream()
                .filter(c -> c.getTags() != null && !c.getTags().isEmpty())
                .collect(Collectors.groupingBy(
                        c -> c.getTags().get(0),
                        Collectors.counting()
                ));
        long totalVisible = allCourses.stream().filter(c -> !c.isHidden()).count();
        categories.put("Toate", totalVisible);
        return categories;
    }

    @GetMapping("/courses/popular")
    public List<Map<String, Object>> getPopularCourses() {
        return courseRepository.findAll().stream()
                .filter(c -> !c.isHidden())
                .limit(4)
                .map(c -> {
                    Map<String, Object> card = new HashMap<>();
                    card.put("title", c.getTitle() != null ? c.getTitle() : "Curs Fără Titlu");
                    card.put("description", c.getDescription());
                    card.put("thumbnailUrl", c.getThumbnailUrl());
                    card.put("avgRating", c.getAvgRating());
                    card.put("enrollmentCount", c.getEnrollmentCount());
                    return card;
                })
                .collect(Collectors.toList());
    }
}
