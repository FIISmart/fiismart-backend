package com.app.service;

import com.app.dto.request.UserRequest;
import com.app.dto.response.UserResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Session;
import com.app.model.User;
import com.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public UserResponse create(UserRequest request) {
        User user = User.builder()
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .role(request.getRole())
                .passwordHash(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()))
                .banned(false)
                .ownedCourses(new ArrayList<>())
                .enrolledCourseIds(new ArrayList<>())
                .sessions(new ArrayList<>())
                .createdAt(new Date())
                .build();
        return toResponse(userRepository.save(user));
    }

    public UserResponse findById(String id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id)));
    }

    public UserResponse findByEmail(String email) {
        return toResponse(userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email)));
    }

    public List<UserResponse> findAllByRole(String role) {
        return userRepository.findByRole(role).stream().map(this::toResponse).toList();
    }

    public List<UserResponse> findAllTeachers() {
        return findAllByRole("teacher");
    }

    public List<UserResponse> findAllStudents() {
        return findAllByRole("student");
    }

    public List<UserResponse> findAllAdmins() {
        return findAllByRole("admin");
    }

    public List<UserResponse> findBannedUsers() {
        return userRepository.findByBanned(true).stream().map(this::toResponse).toList();
    }

    public List<UserResponse> findStudentsEnrolledInCourse(String courseId) {
        return userRepository.findByRoleAndEnrolledCourseIdsContaining("student", courseId)
                .stream().map(this::toResponse).toList();
    }

    public void updateLastLogin(String userId, Date loginTime) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(userId)),
                new Update().set("lastLoginAt", loginTime),
                User.class);
    }

    public void updateDisplayName(String userId, String newName) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(userId)),
                new Update().set("displayName", newName),
                User.class);
    }

    public void updatePasswordHash(String userId, String newHash) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(userId)),
                new Update().set("passwordHash", newHash),
                User.class);
    }

    public void banUser(String userId, String bannedByAdminId, String reason) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(userId)),
                new Update()
                        .set("banned", true)
                        .set("bannedBy", bannedByAdminId)
                        .set("bannedAt", new Date())
                        .set("banReason", reason),
                User.class);
    }

    public void unbanUser(String userId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(userId)),
                new Update()
                        .set("banned", false)
                        .set("bannedBy", null)
                        .set("bannedAt", null)
                        .set("banReason", null),
                User.class);
    }

    public void addOwnedCourse(String teacherId, String courseId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(teacherId)),
                new Update().addToSet("ownedCourses", courseId),
                User.class);
    }

    public void removeOwnedCourse(String teacherId, String courseId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(teacherId)),
                new Update().pull("ownedCourses", courseId),
                User.class);
    }

    public void addEnrolledCourse(String studentId, String courseId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(studentId)),
                new Update().addToSet("enrolledCourseIds", courseId),
                User.class);
    }

    public void removeEnrolledCourse(String studentId, String courseId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(studentId)),
                new Update().pull("enrolledCourseIds", courseId),
                User.class);
    }

    public void addSession(String studentId, Session session) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(studentId)),
                new Update().push("sessions", session),
                User.class);
    }

    public void removeSession(String studentId, String sessionToken) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(studentId)),
                new Update().pull("sessions", new org.bson.Document("token", sessionToken)),
                User.class);
    }

    public void deleteById(String userId) {
        userRepository.deleteById(userId);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isEnrolledInCourse(String studentId, String courseId) {
        return mongoTemplate.exists(
                Query.query(Criteria.where("id").is(studentId).and("enrolledCourseIds").is(courseId)),
                User.class);
    }

    public long countByRole(String role) {
        return userRepository.countByRole(role);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .role(user.getRole())
                .banned(user.isBanned())
                .bannedBy(user.getBannedBy())
                .bannedAt(user.getBannedAt())
                .banReason(user.getBanReason())
                .ownedCourses(user.getOwnedCourses())
                .enrolledCourseIds(user.getEnrolledCourseIds())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
