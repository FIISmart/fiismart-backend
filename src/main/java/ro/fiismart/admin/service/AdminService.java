package ro.fiismart.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.fiismart.admin.dto.*;
import ro.fiismart.auth.service.CognitoAdminService;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.*;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CommentRepository commentRepository;
    private final CognitoAdminService cognitoAdminService;

    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole("student");
        long totalProfessors = userRepository.countByRole("professor");
        long totalAdmins = userRepository.countByRole("admin");
        long totalCourses = courseRepository.count();
        long publishedCourses = courseRepository.findByStatusAndHiddenFalse("published").size();
        long totalEnrollments = enrollmentRepository.count();
        long totalComments = commentRepository.count();

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalStudents(totalStudents)
                .totalProfessors(totalProfessors)
                .totalAdmins(totalAdmins)
                .totalCourses(totalCourses)
                .publishedCourses(publishedCourses)
                .totalEnrollments(totalEnrollments)
                .totalComments(totalComments)
                .build();
    }

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    public AdminUserResponse updateUser(String userId, AdminUpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizatorul nu există"));

        if (req.getDisplayName() != null && !req.getDisplayName().isBlank()) {
            user.setDisplayName(req.getDisplayName().trim());
        }
        if (req.getIsAdmin() != null) {
            String currentRole = user.getRole() != null ? user.getRole().toLowerCase() : "student";
            String cognitoUsername = user.getCognitoUsername() != null
                    ? user.getCognitoUsername() : user.getEmail();
            if (Boolean.TRUE.equals(req.getIsAdmin())) {
                if (!currentRole.equals("admin")) {
                    user.setBaseRole(currentRole);
                    user.setRole("admin");
                    cognitoAdminService.addUserToGroup(cognitoUsername, "ADMIN");
                }
            } else {
                if (currentRole.equals("admin")) {
                    String restored = user.getBaseRole() != null ? user.getBaseRole() : "student";
                    user.setRole(restored);
                    user.setBaseRole(null);
                    cognitoAdminService.removeUserFromGroup(cognitoUsername, "ADMIN");
                }
            }
        }
        if (req.getBanned() != null) {
            user.setBanned(req.getBanned());
            if (req.getBanned()) {
                user.setBanReason(req.getBanReason());
                user.setBannedAt(new Date());
            } else {
                user.setBanReason(null);
                user.setBannedAt(null);
            }
        }

        return AdminUserResponse.from(userRepository.save(user));
    }

    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizatorul nu există"));

        String cognitoUsername = user.getCognitoUsername() != null
                ? user.getCognitoUsername() : user.getEmail();
        if (cognitoUsername != null) {
            cognitoAdminService.deleteUser(cognitoUsername);
        }

        userRepository.deleteById(userId);
    }

    public List<AdminCourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(AdminCourseResponse::from)
                .toList();
    }

    public AdminCourseResponse updateCourse(String courseId, AdminUpdateCourseRequest req) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cursul nu există"));

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            course.setTitle(req.getTitle().trim());
        }
        if (req.getDescription() != null) {
            course.setDescription(req.getDescription());
        }
        if (req.getStatus() != null) {
            course.setStatus(req.getStatus());
        }
        if (req.getHidden() != null) {
            course.setHidden(req.getHidden());
        }
        if (req.getTags() != null) {
            course.setTags(req.getTags());
        }
        course.setUpdatedAt(new Date());

        return AdminCourseResponse.from(courseRepository.save(course));
    }

    public void deleteCourse(String courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cursul nu există");
        }
        courseRepository.deleteById(courseId);
        enrollmentRepository.deleteByCourseId(courseId);
    }

    public List<AdminEnrollmentResponse> getAllEnrollments() {
        return enrollmentRepository.findAll().stream()
                .map(AdminEnrollmentResponse::from)
                .toList();
    }

    public void deleteEnrollment(String enrollmentId) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Înrolarea nu există");
        }
        enrollmentRepository.deleteById(enrollmentId);
    }
}
