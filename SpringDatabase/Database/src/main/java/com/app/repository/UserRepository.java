package com.app.repository;

import com.app.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(String role);

    List<User> findByBanned(boolean banned);

    List<User> findByRoleAndEnrolledCourseIdsContaining(String role, String courseId);

    boolean existsByEmail(String email);

    long countByRole(String role);
}
