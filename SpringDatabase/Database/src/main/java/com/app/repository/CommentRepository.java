package com.app.repository;

import com.app.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findByLectureIdAndDeletedFalse(String lectureId);

    List<Comment> findByLectureIdAndParentCommentIdIsNullAndDeletedFalse(String lectureId);

    List<Comment> findByParentCommentIdAndDeletedFalse(String parentCommentId);

    List<Comment> findByAuthorIdAndDeletedFalse(String authorId);

    List<Comment> findByFlagCountGreaterThanEqualAndDeletedFalse(int minFlags);

    long countByLectureIdAndDeletedFalse(String lectureId);

    void deleteByLectureId(String lectureId);

    void deleteByCourseId(String courseId);
}
