package com.app.repository;

import com.app.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CourseRepository extends MongoRepository<Course, String> {

    List<Course> findByTeacherId(String teacherId);

    List<Course> findByStatusAndHidden(String status, boolean hidden);

    List<Course> findByTagsContaining(String tag);

    List<Course> findByAvgRatingGreaterThanEqual(double minRating);

    long countByTeacherId(String teacherId);
}
