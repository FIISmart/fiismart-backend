package ro.fiismart.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.users.dto.PublicUserProfileResponse;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public PublicUserProfileResponse getPublicProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        long publishedCourses = courseRepository.findByTeacherId(userId).stream()
                .filter(course -> "published".equalsIgnoreCase(nullSafe(course.getStatus())))
                .filter(course -> !Boolean.TRUE.equals(course.isHidden()))
                .map(Course::getId)
                .count();

        return PublicUserProfileResponse.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .role(normalizeRole(user.getRole()))
                .avatarUrl(user.getAvatarUrl())
                .headline(user.getHeadline())
                .bio(user.getBio())
                .faculty(user.getFaculty())
                .specialization(user.getSpecialization())
                .studyYear(user.getStudyYear())
                .educationLevel(user.getEducationLevel())
                .department(user.getDepartment())
                .academicTitle(user.getAcademicTitle())
                .interests(user.getInterests())
                .subjects(user.getSubjects())
                .tutorProfileEnabled(user.getTutorProfileEnabled())
                .tutorRating(user.getTutorRating())
                .tutorReviewCount(user.getTutorReviewCount())
                .experienceYears(user.getExperienceYears())
                .availability(user.getAvailability())
                .priceLabel(user.getPriceLabel())
                .publishedCourseCount(publishedCourses)
                .build();
    }

    private String normalizeRole(String role) {
        String raw = nullSafe(role).toLowerCase();
        if ("admin".equals(raw)) return "ADMIN";
        if ("professor".equals(raw) || "teacher".equals(raw)) return "PROFESSOR";
        return "STUDENT";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
