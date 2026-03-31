package com.app.service;

import com.app.dto.request.CommentRequest;
import com.app.dto.response.CommentResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Comment;
import com.app.model.ModerationFlag;
import com.app.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private CommentService commentService;

    private Comment buildComment(String id, String lectureId, String courseId, String authorId, String body) {
        return Comment.builder()
                .id(id)
                .lectureId(lectureId)
                .courseId(courseId)
                .authorId(authorId)
                .body(body)
                .createdAt(new Date())
                .updatedAt(new Date())
                .deleted(false)
                .likeCount(0)
                .flagCount(0)
                .likedBy(new ArrayList<>())
                .moderationFlags(new ArrayList<>())
                .build();
    }

    @Test
    void create_savesCommentAndReturnsResponse() {
        CommentRequest request = CommentRequest.builder()
                .lectureId("lec1")
                .courseId("c1")
                .authorId("u1")
                .body("Great lecture!")
                .build();

        Comment saved = buildComment("cm1", "lec1", "c1", "u1", "Great lecture!");
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        CommentResponse response = commentService.create(request);

        assertThat(response.getId()).isEqualTo("cm1");
        assertThat(response.getLectureId()).isEqualTo("lec1");
        assertThat(response.getBody()).isEqualTo("Great lecture!");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void create_setsInitialValues() {
        CommentRequest request = CommentRequest.builder()
                .lectureId("lec1")
                .courseId("c1")
                .authorId("u1")
                .body("Nice!")
                .build();

        Comment saved = buildComment("cm1", "lec1", "c1", "u1", "Nice!");
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        commentService.create(request);
        verify(commentRepository).save(captor.capture());

        assertThat(captor.getValue().isDeleted()).isFalse();
        assertThat(captor.getValue().getLikeCount()).isEqualTo(0);
        assertThat(captor.getValue().getFlagCount()).isEqualTo(0);
        assertThat(captor.getValue().getLikedBy()).isNotNull().isEmpty();
        assertThat(captor.getValue().getModerationFlags()).isNotNull().isEmpty();
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void create_withParentCommentId_setsParentCommentId() {
        CommentRequest request = CommentRequest.builder()
                .lectureId("lec1")
                .courseId("c1")
                .authorId("u1")
                .body("Reply!")
                .parentCommentId("parent1")
                .build();

        Comment saved = buildComment("cm2", "lec1", "c1", "u1", "Reply!");
        saved.setParentCommentId("parent1");
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        commentService.create(request);
        verify(commentRepository).save(captor.capture());

        assertThat(captor.getValue().getParentCommentId()).isEqualTo("parent1");
    }

    @Test
    void create_withNullParentCommentId_setsNullParent() {
        CommentRequest request = CommentRequest.builder()
                .lectureId("lec1")
                .courseId("c1")
                .authorId("u1")
                .body("Top level")
                .parentCommentId(null)
                .build();

        Comment saved = buildComment("cm3", "lec1", "c1", "u1", "Top level");
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        commentService.create(request);
        verify(commentRepository).save(captor.capture());

        assertThat(captor.getValue().getParentCommentId()).isNull();
    }

    @Test
    void findById_returnsResponse_whenFound() {
        Comment comment = buildComment("cm1", "lec1", "c1", "u1", "Body");
        when(commentRepository.findById("cm1")).thenReturn(Optional.of(comment));

        CommentResponse response = commentService.findById("cm1");

        assertThat(response.getId()).isEqualTo("cm1");
    }

    @Test
    void findById_throwsResourceNotFoundException_whenNotFound() {
        when(commentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment")
                .hasMessageContaining("missing");
    }

    @Test
    void findByLectureId_returnsNotDeletedComments() {
        List<Comment> comments = List.of(
                buildComment("cm1", "lec1", "c1", "u1", "Body1"),
                buildComment("cm2", "lec1", "c1", "u2", "Body2")
        );
        when(commentRepository.findByLectureIdAndDeletedFalse("lec1")).thenReturn(comments);

        List<CommentResponse> responses = commentService.findByLectureId("lec1");

        assertThat(responses).hasSize(2);
    }

    @Test
    void findByLectureId_returnsEmptyList_whenNone() {
        when(commentRepository.findByLectureIdAndDeletedFalse("lec99")).thenReturn(List.of());

        assertThat(commentService.findByLectureId("lec99")).isEmpty();
    }

    @Test
    void findTopLevelByLectureId_returnsTopLevelComments() {
        Comment topLevel = buildComment("cm1", "lec1", "c1", "u1", "Top");
        when(commentRepository.findByLectureIdAndParentCommentIdIsNullAndDeletedFalse("lec1"))
                .thenReturn(List.of(topLevel));

        List<CommentResponse> responses = commentService.findTopLevelByLectureId("lec1");

        assertThat(responses).hasSize(1);
        verify(commentRepository).findByLectureIdAndParentCommentIdIsNullAndDeletedFalse("lec1");
    }

    @Test
    void findRepliesByParentId_returnsReplies() {
        Comment reply = buildComment("cm2", "lec1", "c1", "u2", "Reply");
        reply.setParentCommentId("cm1");
        when(commentRepository.findByParentCommentIdAndDeletedFalse("cm1")).thenReturn(List.of(reply));

        List<CommentResponse> responses = commentService.findRepliesByParentId("cm1");

        assertThat(responses).hasSize(1);
    }

    @Test
    void findByAuthorId_returnsAuthorComments() {
        List<Comment> comments = List.of(buildComment("cm1", "lec1", "c1", "u1", "A comment"));
        when(commentRepository.findByAuthorIdAndDeletedFalse("u1")).thenReturn(comments);

        List<CommentResponse> responses = commentService.findByAuthorId("u1");

        assertThat(responses).hasSize(1);
    }

    @Test
    void findFlagged_returnsFlaggedComments() {
        Comment flagged = buildComment("cm1", "lec1", "c1", "u1", "Bad comment");
        flagged.setFlagCount(3);
        when(commentRepository.findByFlagCountGreaterThanEqualAndDeletedFalse(3))
                .thenReturn(List.of(flagged));

        List<CommentResponse> responses = commentService.findFlagged(3);

        assertThat(responses).hasSize(1);
    }

    @Test
    void findFlagged_returnsEmptyList_whenNoneMatchThreshold() {
        when(commentRepository.findByFlagCountGreaterThanEqualAndDeletedFalse(10)).thenReturn(List.of());

        assertThat(commentService.findFlagged(10)).isEmpty();
    }

    @Test
    void updateBody_invokesMongoTemplate() {
        commentService.updateBody("cm1", "Updated body");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Comment.class));
    }

    @Test
    void softDelete_invokesMongoTemplate() {
        commentService.softDelete("cm1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Comment.class));
    }

    @Test
    void addLike_invokesMongoTemplate() {
        commentService.addLike("cm1", "u1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Comment.class));
    }

    @Test
    void removeLike_invokesMongoTemplate() {
        commentService.removeLike("cm1", "u1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Comment.class));
    }

    @Test
    void addModerationFlag_invokesMongoTemplate() {
        ModerationFlag flag = ModerationFlag.builder()
                .flaggedBy("mod1")
                .reason("Inappropriate")
                .flaggedAt(new Date())
                .build();

        commentService.addModerationFlag("cm1", flag);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Comment.class));
    }

    @Test
    void clearModerationFlags_invokesMongoTemplate() {
        commentService.clearModerationFlags("cm1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Comment.class));
    }

    @Test
    void deleteById_delegatesToRepository() {
        commentService.deleteById("cm1");

        verify(commentRepository).deleteById("cm1");
    }

    @Test
    void deleteAllByLecture_delegatesToRepository() {
        commentService.deleteAllByLecture("lec1");

        verify(commentRepository).deleteByLectureId("lec1");
    }

    @Test
    void deleteAllByCourse_delegatesToRepository() {
        commentService.deleteAllByCourse("c1");

        verify(commentRepository).deleteByCourseId("c1");
    }

    @Test
    void countByLecture_returnsCount() {
        when(commentRepository.countByLectureIdAndDeletedFalse("lec1")).thenReturn(7L);

        assertThat(commentService.countByLecture("lec1")).isEqualTo(7L);
    }

    @Test
    void countByLecture_returnsZero_whenNone() {
        when(commentRepository.countByLectureIdAndDeletedFalse("lec99")).thenReturn(0L);

        assertThat(commentService.countByLecture("lec99")).isEqualTo(0L);
    }

    @Test
    void hasUserLiked_returnsTrue_whenLiked() {
        when(mongoTemplate.exists(any(Query.class), eq(Comment.class))).thenReturn(true);

        assertThat(commentService.hasUserLiked("cm1", "u1")).isTrue();
    }

    @Test
    void hasUserLiked_returnsFalse_whenNotLiked() {
        when(mongoTemplate.exists(any(Query.class), eq(Comment.class))).thenReturn(false);

        assertThat(commentService.hasUserLiked("cm1", "u1")).isFalse();
    }

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        Date created = new Date();
        Date updated = new Date();
        ModerationFlag flag = ModerationFlag.builder()
                .flaggedBy("mod1")
                .reason("Spam")
                .flaggedAt(new Date())
                .build();

        Comment comment = Comment.builder()
                .id("cm1")
                .lectureId("lec1")
                .courseId("c1")
                .authorId("u1")
                .body("Hello world")
                .createdAt(created)
                .updatedAt(updated)
                .deleted(false)
                .parentCommentId("parent1")
                .likeCount(5)
                .likedBy(List.of("u2", "u3"))
                .moderationFlags(List.of(flag))
                .flagCount(1)
                .build();

        when(commentRepository.findById("cm1")).thenReturn(Optional.of(comment));

        CommentResponse response = commentService.findById("cm1");

        assertThat(response.getId()).isEqualTo("cm1");
        assertThat(response.getLectureId()).isEqualTo("lec1");
        assertThat(response.getCourseId()).isEqualTo("c1");
        assertThat(response.getAuthorId()).isEqualTo("u1");
        assertThat(response.getBody()).isEqualTo("Hello world");
        assertThat(response.getCreatedAt()).isEqualTo(created);
        assertThat(response.getUpdatedAt()).isEqualTo(updated);
        assertThat(response.isDeleted()).isFalse();
        assertThat(response.getParentCommentId()).isEqualTo("parent1");
        assertThat(response.getLikeCount()).isEqualTo(5);
        assertThat(response.getLikedBy()).containsExactly("u2", "u3");
        assertThat(response.getModerationFlags()).hasSize(1);
        assertThat(response.getFlagCount()).isEqualTo(1);
    }

    @Test
    void toResponse_withDeletedComment_returnsDeletedTrue() {
        Comment comment = buildComment("cm1", "lec1", "c1", "u1", "deleted");
        comment.setDeleted(true);
        when(commentRepository.findById("cm1")).thenReturn(Optional.of(comment));

        CommentResponse response = commentService.findById("cm1");

        assertThat(response.isDeleted()).isTrue();
    }
}
