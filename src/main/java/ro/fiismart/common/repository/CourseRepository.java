package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.common.model.Course;

import java.util.List;

public interface CourseRepository extends MongoRepository<Course, String> {

    List<Course> findByTeacherId(String teacherId);

    List<Course> findByStatusAndHiddenFalse(String status);

    List<Course> findByTagsContaining(String tag);
}
