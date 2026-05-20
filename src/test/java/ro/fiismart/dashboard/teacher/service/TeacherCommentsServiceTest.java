package ro.fiismart.dashboard.teacher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.model.Comment;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Lecture;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CommentRepository;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.dashboard.teacher.dto.TeacherCommentPreviewDTO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherCommentsServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private TeacherCommentsService teacherCommentsService;

    @Test
    void getComments_noCoursesForTeacher_returnsEmpty() {
        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of());

        List<TeacherCommentPreviewDTO> result = teacherCommentsService.getComments("teacher1", 10, 0);

        assertThat(result).isEmpty();
    }

    @Test
    void getComments_courseWithNoModules_returnsEmpty() {
        Course course = Course.builder().id("c1").modules(null).build();
        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(course));

        List<TeacherCommentPreviewDTO> result = teacherCommentsService.getComments("teacher1", 10, 0);

        assertThat(result).isEmpty();
    }

    @Test
    void getComments_withComments_returnsMappedDTOs() {
        Lecture lecture = Lecture.builder().id("lec1").build();
        CourseModule module = CourseModule.builder()
                .lectures(new ArrayList<>(List.of(lecture))).build();
        Course course = Course.builder()
                .id("c1").title("Matematică")
                .modules(new ArrayList<>(List.of(module))).build();

        Comment comment = Comment.builder()
                .id("com1").authorId("student1").courseId("c1").lectureId("lec1")
                .body("Buna intrebare").likeCount(2).createdAt(new Date()).build();
        User author = User.builder().id("student1").displayName("Ion").build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(course));
        when(commentRepository.findTopLevelByLectureId("lec1")).thenReturn(List.of(comment));
        when(commentRepository.findRepliesByParentId("com1")).thenReturn(List.of());
        when(userRepository.findById("student1")).thenReturn(Optional.of(author));

        List<TeacherCommentPreviewDTO> result = teacherCommentsService.getComments("teacher1", 10, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCommentId()).isEqualTo("com1");
        assertThat(result.get(0).getAuthorDisplayName()).isEqualTo("Ion");
        assertThat(result.get(0).getLikeCount()).isEqualTo(2);
        assertThat(result.get(0).isAnswered()).isFalse();
    }

    @Test
    void getComments_withReplies_isAnsweredTrue() {
        Lecture lecture = Lecture.builder().id("lec1").build();
        CourseModule module = CourseModule.builder()
                .lectures(new ArrayList<>(List.of(lecture))).build();
        Course course = Course.builder().id("c1")
                .modules(new ArrayList<>(List.of(module))).build();

        Comment comment = Comment.builder()
                .id("com1").authorId("student1").courseId("c1").lectureId("lec1")
                .body("Intrebare").likeCount(0).createdAt(new Date()).build();
        Comment reply = Comment.builder().id("rep1").body("Raspuns").build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(course));
        when(commentRepository.findTopLevelByLectureId("lec1")).thenReturn(List.of(comment));
        when(commentRepository.findRepliesByParentId("com1")).thenReturn(List.of(reply));
        when(userRepository.findById("student1")).thenReturn(Optional.empty());

        List<TeacherCommentPreviewDTO> result = teacherCommentsService.getComments("teacher1", 10, 0);

        assertThat(result.get(0).isAnswered()).isTrue();
        assertThat(result.get(0).getRepliesCount()).isEqualTo(1);
        assertThat(result.get(0).getAuthorDisplayName()).isEqualTo("");
    }

    @Test
    void getComments_withPagination_appliesLimitOffset() {
        Lecture lec1 = Lecture.builder().id("lec1").build();
        Lecture lec2 = Lecture.builder().id("lec2").build();
        CourseModule module = CourseModule.builder()
                .lectures(new ArrayList<>(List.of(lec1, lec2))).build();
        Course course = Course.builder().id("c1")
                .modules(new ArrayList<>(List.of(module))).build();

        Comment c1 = Comment.builder().id("com1").authorId("s1").courseId("c1")
                .lectureId("lec1").body("A").likeCount(0).createdAt(new Date()).build();
        Comment c2 = Comment.builder().id("com2").authorId("s1").courseId("c1")
                .lectureId("lec2").body("B").likeCount(0).createdAt(new Date()).build();
        Comment c3 = Comment.builder().id("com3").authorId("s1").courseId("c1")
                .lectureId("lec1").body("C").likeCount(0).createdAt(new Date()).build();

        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(course));
        when(commentRepository.findTopLevelByLectureId("lec1")).thenReturn(List.of(c1, c3));
        when(commentRepository.findTopLevelByLectureId("lec2")).thenReturn(List.of(c2));
        when(commentRepository.findRepliesByParentId(any())).thenReturn(List.of());
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        List<TeacherCommentPreviewDTO> result = teacherCommentsService.getComments("teacher1", 2, 1);

        assertThat(result).hasSize(2);
    }
}
