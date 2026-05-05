package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import ro.fiismart.common.model.Comment;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, String> {

    @Query("{ 'lectureId': ?0, 'isDeleted': false }")
    List<Comment> findByLectureIdAndNotDeleted(String lectureId);

    @Query("{ 'lectureId': ?0, 'parentCommentId': null, 'isDeleted': false }")
    List<Comment> findTopLevelByLectureId(String lectureId);

    @Query("{ 'parentCommentId': ?0, 'isDeleted': false }")
    List<Comment> findRepliesByParentId(String parentCommentId);

    @Query("{ 'authorId': ?0, 'isDeleted': false }")
    List<Comment> findByAuthorId(String authorId);

    long countByLectureIdAndDeletedFalse(String lectureId);

    @Query("{ 'courseId': ?0 }")
    List<Comment> findByCourseId(String courseId);
}
