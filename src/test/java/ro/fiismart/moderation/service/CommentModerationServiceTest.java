package ro.fiismart.moderation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Comment;
import ro.fiismart.common.repository.CommentRepository;
import ro.fiismart.moderation.dto.CommentModerationResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentModerationServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private CommentModerationService commentModerationService;

    private Comment sampleComment;

    @BeforeEach
    void setUp() {
        sampleComment = Comment.builder()
                .id("comment1")
                .authorId("student1")
                .courseId("course1")
                .lectureId("lecture1")
                .body("Test comment")
                .deleted(false)
                .likedBy(new ArrayList<>())
                .build();
    }

    @Test
    void getCommentsByCourseId_returnsMappedList() {
        when(commentRepository.findByCourseId("course1")).thenReturn(List.of(sampleComment));

        List<CommentModerationResponse> result = commentModerationService.getCommentsByCourseId("course1");

        assertThat(result).hasSize(1);
    }

    @Test
    void updateCommentStatus_approved_callsMongoTemplate() {
        when(commentRepository.findById("comment1")).thenReturn(Optional.of(sampleComment));

        commentModerationService.updateCommentStatus("comment1", "approved");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Comment.class));
    }

    @Test
    void updateCommentStatus_rejected_callsMongoTemplate() {
        when(commentRepository.findById("comment1")).thenReturn(Optional.of(sampleComment));

        commentModerationService.updateCommentStatus("comment1", "rejected");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Comment.class));
    }

    @Test
    void updateCommentStatus_pending_callsMongoTemplate() {
        when(commentRepository.findById("comment1")).thenReturn(Optional.of(sampleComment));

        commentModerationService.updateCommentStatus("comment1", "pending");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Comment.class));
    }

    @Test
    void updateCommentStatus_invalidStatus_throwsIllegalArgument() {
        when(commentRepository.findById("comment1")).thenReturn(Optional.of(sampleComment));

        assertThatThrownBy(() -> commentModerationService.updateCommentStatus("comment1", "unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    void updateCommentStatus_commentNotFound_throws() {
        when(commentRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentModerationService.updateCommentStatus("noId", "approved"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteComment_softDeletesComment() {
        when(commentRepository.findById("comment1")).thenReturn(Optional.of(sampleComment));

        commentModerationService.deleteComment("comment1");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Comment.class));
    }

    @Test
    void deleteComment_notFound_throws() {
        when(commentRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentModerationService.deleteComment("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
