package ro.fiismart.dashboard.student.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.fiismart.common.model.Comment;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CommentRepository;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.dashboard.student.dto.CommentCreateRequest;
import ro.fiismart.dashboard.student.dto.StudentCommentDTO;
import ro.fiismart.notification.service.NotificationService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StudentCommentService studentCommentService;

    private User sampleAuthor;
    private Comment sampleComment;
    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        sampleAuthor = User.builder()
                .id("student1")
                .displayName("Ion Popescu")
                .role("student")
                .build();

        sampleComment = Comment.builder()
                .id("comment1")
                .authorId("student1")
                .courseId("course1")
                .lectureId("lecture1")
                .body("Foarte interesant!")
                .likeCount(3)
                .likedBy(new ArrayList<>(List.of("user2", "user3")))
                .createdAt(new Date())
                .deleted(false)
                .build();

        sampleCourse = Course.builder()
                .id("course1")
                .title("Matematică")
                .teacherId("teacher1")
                .build();
    }

    // ── getCommentsThreaded ───────────────────────────────────────────────────

    @Test
    void getCommentsThreaded_returnsTopLevelCommentsSortedByLikes() {
        Comment c1 = Comment.builder().id("c1").authorId("student1").likeCount(5)
                .createdAt(new Date()).likedBy(new ArrayList<>()).build();
        Comment c2 = Comment.builder().id("c2").authorId("student1").likeCount(10)
                .createdAt(new Date()).likedBy(new ArrayList<>()).build();

        when(commentRepository.findByLectureIdAndNotDeleted("lecture1")).thenReturn(List.of(c1, c2));
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleAuthor));

        List<StudentCommentDTO> result = studentCommentService.getCommentsThreaded("student1", "lecture1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLikeCount()).isGreaterThanOrEqualTo(result.get(1).getLikeCount());
    }

    @Test
    void getCommentsThreaded_buildsReplyTree() {
        Comment parent = Comment.builder().id("parent1").authorId("student1").likeCount(0)
                .createdAt(new Date()).likedBy(new ArrayList<>()).build();
        Comment reply = Comment.builder().id("reply1").authorId("student1")
                .parentCommentId("parent1").likeCount(0)
                .createdAt(new Date()).likedBy(new ArrayList<>()).build();

        when(commentRepository.findByLectureIdAndNotDeleted("lecture1")).thenReturn(List.of(parent, reply));
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleAuthor));

        List<StudentCommentDTO> result = studentCommentService.getCommentsThreaded("student1", "lecture1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCommentId()).isEqualTo("parent1");
        assertThat(result.get(0).getReplies()).hasSize(1);
        assertThat(result.get(0).getReplies().get(0).getCommentId()).isEqualTo("reply1");
    }

    @Test
    void getCommentsThreaded_marksLikedByMe() {
        sampleComment.setLikedBy(new ArrayList<>(List.of("student1")));
        when(commentRepository.findByLectureIdAndNotDeleted("lecture1")).thenReturn(List.of(sampleComment));
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleAuthor));

        List<StudentCommentDTO> result = studentCommentService.getCommentsThreaded("student1", "lecture1");

        assertThat(result.get(0).isLikedByMe()).isTrue();
    }

    @Test
    void getCommentsThreaded_unknownAuthor_usesDefault() {
        when(commentRepository.findByLectureIdAndNotDeleted("lecture1")).thenReturn(List.of(sampleComment));
        when(userRepository.findById("student1")).thenReturn(Optional.empty());

        List<StudentCommentDTO> result = studentCommentService.getCommentsThreaded("student1", "lecture1");

        assertThat(result.get(0).getAuthorName()).isEqualTo("Utilizator necunoscut");
    }

    @Test
    void getCommentsThreaded_teacherRole_showsProfesor() {
        sampleAuthor.setRole("teacher");
        when(commentRepository.findByLectureIdAndNotDeleted("lecture1")).thenReturn(List.of(sampleComment));
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleAuthor));

        List<StudentCommentDTO> result = studentCommentService.getCommentsThreaded("student1", "lecture1");

        assertThat(result.get(0).getAuthorRole()).isEqualTo("Profesor");
    }

    @Test
    void getCommentsThreaded_adminRole_showsAdmin() {
        sampleAuthor.setRole("admin");
        when(commentRepository.findByLectureIdAndNotDeleted("lecture1")).thenReturn(List.of(sampleComment));
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleAuthor));

        List<StudentCommentDTO> result = studentCommentService.getCommentsThreaded("student1", "lecture1");

        assertThat(result.get(0).getAuthorRole()).isEqualTo("Admin");
    }

    // ── createComment ─────────────────────────────────────────────────────────

    @Test
    void createComment_savesAndNotifiesTeacher() {
        CommentCreateRequest req = new CommentCreateRequest();
        req.setBody("Buna intrebare!");
        req.setPositionSecs(30);

        when(commentRepository.save(any(Comment.class))).thenReturn(sampleComment);
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleAuthor));
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        StudentCommentDTO result = studentCommentService.createComment("student1", "course1", "lecture1", req);

        assertThat(result).isNotNull();
        verify(notificationService).createCommentNotification(
                "teacher1", "Ion Popescu", "Matematică", "course1", "lecture1");
    }

    @Test
    void createComment_noCourse_doesNotNotify() {
        CommentCreateRequest req = new CommentCreateRequest();
        req.setBody("Test");

        when(commentRepository.save(any(Comment.class))).thenReturn(sampleComment);
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleAuthor));
        when(courseRepository.findById("course1")).thenReturn(Optional.empty());

        studentCommentService.createComment("student1", "course1", "lecture1", req);

        verify(notificationService, never()).createCommentNotification(any(), any(), any(), any(), any());
    }

    @Test
    void createComment_courseWithNoTeacher_doesNotNotify() {
        sampleCourse.setTeacherId(null);
        CommentCreateRequest req = new CommentCreateRequest();
        req.setBody("Test");

        when(commentRepository.save(any(Comment.class))).thenReturn(sampleComment);
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleAuthor));
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        studentCommentService.createComment("student1", "course1", "lecture1", req);

        verify(notificationService, never()).createCommentNotification(any(), any(), any(), any(), any());
    }

    // ── replyToComment ────────────────────────────────────────────────────────

    @Test
    void replyToComment_savesReplyAndNotifiesParentAuthor() {
        Comment parentComment = Comment.builder()
                .id("parent1").authorId("otherUser").courseId("course1")
                .lectureId("lecture1").likedBy(new ArrayList<>()).build();
        Comment savedReply = Comment.builder()
                .id("reply1").authorId("student1").courseId("course1")
                .lectureId("lecture1").likedBy(new ArrayList<>()).createdAt(new Date()).build();

        CommentCreateRequest req = new CommentCreateRequest();
        req.setBody("Raspuns");

        when(commentRepository.findById("parent1")).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedReply);
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleAuthor));

        StudentCommentDTO result = studentCommentService.replyToComment("student1", "parent1", req);

        assertThat(result).isNotNull();
        verify(notificationService).createReplyNotification("otherUser", "Ion Popescu", "course1");
    }

    @Test
    void replyToComment_replyingToSelf_doesNotNotify() {
        Comment parentComment = Comment.builder()
                .id("parent1").authorId("student1").courseId("course1")
                .lectureId("lecture1").likedBy(new ArrayList<>()).build();
        Comment savedReply = Comment.builder()
                .id("reply1").authorId("student1").courseId("course1")
                .lectureId("lecture1").likedBy(new ArrayList<>()).createdAt(new Date()).build();

        CommentCreateRequest req = new CommentCreateRequest();
        req.setBody("Self reply");

        when(commentRepository.findById("parent1")).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedReply);
        when(userRepository.findById("student1")).thenReturn(Optional.of(sampleAuthor));

        studentCommentService.replyToComment("student1", "parent1", req);

        verify(notificationService, never()).createReplyNotification(any(), any(), any());
    }

    @Test
    void replyToComment_parentNotFound_throws() {
        when(commentRepository.findById("noId")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                studentCommentService.replyToComment("student1", "noId", new CommentCreateRequest()));
    }

    // ── toggleLike ────────────────────────────────────────────────────────────

    @Test
    void toggleLike_addLike_callsMongoTemplateWithAddToSet() {
        sampleComment.setLikedBy(new ArrayList<>());
        when(commentRepository.findById("comment1")).thenReturn(Optional.of(sampleComment));

        studentCommentService.toggleLike("newUser", "comment1");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Comment.class));
    }

    @Test
    void toggleLike_removeLike_callsMongoTemplateWithPull() {
        sampleComment.setLikedBy(new ArrayList<>(List.of("student1")));
        when(commentRepository.findById("comment1")).thenReturn(Optional.of(sampleComment));

        studentCommentService.toggleLike("student1", "comment1");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Comment.class));
    }

    @Test
    void toggleLike_commentNotFound_doesNothing() {
        when(commentRepository.findById("noId")).thenReturn(Optional.empty());

        studentCommentService.toggleLike("student1", "noId");

        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(Comment.class));
    }

    @Test
    void toggleLike_nullLikedBy_treatsAsNotLiked() {
        sampleComment.setLikedBy(null);
        when(commentRepository.findById("comment1")).thenReturn(Optional.of(sampleComment));

        studentCommentService.toggleLike("student1", "comment1");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Comment.class));
    }
}
