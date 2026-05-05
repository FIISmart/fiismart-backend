package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.common.model.ModuleQuiz;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link ModuleQuiz}. Lookups are scoped by the
 * {@code quizScope} discriminator so a single collection holds lecture,
 * module, and course-final quizzes without ambiguity.
 */
public interface ModuleQuizRepository extends MongoRepository<ModuleQuiz, String> {

    Optional<ModuleQuiz> findByLectureIdAndQuizScope(String lectureId, String quizScope);

    Optional<ModuleQuiz> findByModuleIdAndQuizScope(String moduleId, String quizScope);

    Optional<ModuleQuiz> findByCourseIdAndQuizScope(String courseId, String quizScope);

    List<ModuleQuiz> findAllByCourseId(String courseId);

    List<ModuleQuiz> findAllByModuleId(String moduleId);

    boolean existsByLectureIdAndQuizScope(String lectureId, String quizScope);

    boolean existsByModuleIdAndQuizScope(String moduleId, String quizScope);

    boolean existsByCourseIdAndQuizScope(String courseId, String quizScope);

    void deleteByLectureIdAndQuizScope(String lectureId, String quizScope);

    void deleteByModuleIdAndQuizScope(String moduleId, String quizScope);

    void deleteByCourseIdAndQuizScope(String courseId, String quizScope);
}
