package ro.fiismart.tutors.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.tutors.dto.TutorResponse;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TutorService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public TutorService(UserRepository userRepository, CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public List<TutorResponse> listTutors() {
        return userRepository.findAll().stream()
                .filter(user -> "professor".equalsIgnoreCase(user.getRole()) || "teacher".equalsIgnoreCase(user.getRole()))
                .filter(user -> !user.isBanned())
                .map(this::toResponse)
                .sorted(Comparator.comparing(TutorResponse::getPublishedCourseCount).reversed()
                        .thenComparing(TutorResponse::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private TutorResponse toResponse(User user) {
        List<Course> courses = courseRepository.findByTeacherId(user.getId());
        List<Course> publishedCourses = courses.stream()
                .filter(course -> "published".equalsIgnoreCase(course.getStatus()) && !course.isHidden())
                .toList();

        Set<String> tags = new LinkedHashSet<>();
        if (user.getSubjects() != null) tags.addAll(user.getSubjects());
        for (Course course : publishedCourses) {
            if (course.getTags() != null) tags.addAll(course.getTags());
        }

        double avgRating = publishedCourses.stream()
                .mapToDouble(Course::getAvgRating)
                .filter(rating -> rating > 0)
                .average()
                .orElse(0);

        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = "Profesor FII Smart";
        }

        String bio = user.getBio();
        if (bio == null || bio.isBlank()) {
            bio = publishedCourses.isEmpty()
                    ? "Profesor inregistrat pe FII Smart."
                    : "Preda " + publishedCourses.size() + " curs(uri) publicate pe FII Smart.";
        }

        double rating = user.getTutorRating() != null && user.getTutorRating() > 0
                ? user.getTutorRating()
                : avgRating;

        return TutorResponse.builder()
                .id(user.getId())
                .displayName(displayName)
                .headline(user.getHeadline())
                .tags(tags.stream().limit(6).toList())
                .bio(bio)
                .courseCount(courses.size())
                .publishedCourseCount(publishedCourses.size())
                .avgRating(Math.round(rating * 10.0) / 10.0)
                .reviewCount(user.getTutorReviewCount() != null ? user.getTutorReviewCount() : 0)
                .experienceYears(user.getExperienceYears() != null ? user.getExperienceYears() : 0)
                .avatarUrl(user.getAvatarUrl())
                .availability(user.getAvailability())
                .priceLabel(user.getPriceLabel())
                .build();
    }
}
