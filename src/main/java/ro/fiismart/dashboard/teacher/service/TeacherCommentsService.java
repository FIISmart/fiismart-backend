package ro.fiismart.dashboard.teacher.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.Comment;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Lecture;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CommentRepository;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.dashboard.teacher.dto.TeacherCommentPreviewDTO;

import java.util.*;

@Service
public class TeacherCommentsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CommentRepository commentRepository;

    public TeacherCommentsService(UserRepository userRepository,
                                  CourseRepository courseRepository,
                                  CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.commentRepository = commentRepository;
    }

    public List<TeacherCommentPreviewDTO> getComments(String teacherId, int limit, int offset) {
        List<Course> courses = courseRepository.findByTeacherId(teacherId);

        Map<String, String> courseTitles = new HashMap<>();
        List<String> lectureIds = new ArrayList<>();

        for (Course c : courses) {
            courseTitles.put(c.getId(), c.getTitle());
            if (c.getModules() != null) {
                for (CourseModule module : c.getModules()) {
                    if (module.getLectures() != null) {
                        for (Lecture lecture : module.getLectures()) {
                            lectureIds.add(lecture.getId());
                        }
                    }
                }
            }
        }

        List<TeacherCommentPreviewDTO> result = new ArrayList<>();
        for (String lectureId : lectureIds) {
            List<Comment> comments = commentRepository.findTopLevelByLectureId(lectureId);
            for (Comment comment : comments) {
                List<Comment> replies = commentRepository.findRepliesByParentId(comment.getId());
                User author = userRepository.findById(comment.getAuthorId()).orElse(null);

                TeacherCommentPreviewDTO dto = new TeacherCommentPreviewDTO();
                dto.setCommentId(comment.getId());
                dto.setCourseId(comment.getCourseId());
                dto.setCourseTitle(courseTitles.get(comment.getCourseId()));
                dto.setLectureId(comment.getLectureId());
                dto.setAuthorId(comment.getAuthorId());
                dto.setAuthorDisplayName(author != null ? author.getDisplayName() : "");
                dto.setBody(comment.getBody());
                dto.setCreatedAt(comment.getCreatedAt());
                dto.setLikeCount(comment.getLikeCount());
                dto.setRepliesCount(replies.size());
                dto.setAnswered(!replies.isEmpty());
                dto.setStatus(comment.getStatus() != null ? comment.getStatus() : (!replies.isEmpty() ? "ANSWERED" : "OPEN"));
                result.add(dto);
            }
        }

        return result.stream().skip(offset).limit(limit).toList();
    }

    public TeacherCommentPreviewDTO reply(String teacherId, String commentId, String body) {
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found"));
        ensureTeacherOwnsCourse(teacherId, parent.getCourseId());
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Raspunsul nu poate fi gol");
        }

        Comment reply = Comment.builder()
                .courseId(parent.getCourseId())
                .lectureId(parent.getLectureId())
                .authorId(teacherId)
                .parentCommentId(parent.getId())
                .body(body.trim())
                .status("ANSWERED")
                .createdAt(new Date())
                .updatedAt(new Date())
                .deleted(false)
                .build();
        commentRepository.save(reply);
        parent.setStatus("ANSWERED");
        parent.setUpdatedAt(new Date());
        commentRepository.save(parent);
        return toPreview(parent);
    }

    public TeacherCommentPreviewDTO updateStatus(String teacherId, String commentId, String status) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found"));
        ensureTeacherOwnsCourse(teacherId, comment.getCourseId());
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("OPEN", "ANSWERED", "RESOLVED").contains(normalized)) {
            throw new IllegalArgumentException("Status invalid");
        }
        comment.setStatus(normalized);
        comment.setUpdatedAt(new Date());
        commentRepository.save(comment);
        return toPreview(comment);
    }

    private void ensureTeacherOwnsCourse(String teacherId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found"));
        if (!teacherId.equals(course.getTeacherId())) {
            throw new SecurityException("Nu poti modifica comentarii pentru acest curs");
        }
    }

    private TeacherCommentPreviewDTO toPreview(Comment comment) {
        User author = userRepository.findById(comment.getAuthorId()).orElse(null);
        Course course = courseRepository.findById(comment.getCourseId()).orElse(null);
        List<Comment> replies = commentRepository.findRepliesByParentId(comment.getId());

        TeacherCommentPreviewDTO dto = new TeacherCommentPreviewDTO();
        dto.setCommentId(comment.getId());
        dto.setCourseId(comment.getCourseId());
        dto.setCourseTitle(course != null ? course.getTitle() : "");
        dto.setLectureId(comment.getLectureId());
        dto.setAuthorId(comment.getAuthorId());
        dto.setAuthorDisplayName(author != null ? author.getDisplayName() : "");
        dto.setBody(comment.getBody());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setLikeCount(comment.getLikeCount());
        dto.setRepliesCount(replies.size());
        dto.setAnswered(!replies.isEmpty());
        dto.setStatus(comment.getStatus() != null ? comment.getStatus() : (!replies.isEmpty() ? "ANSWERED" : "OPEN"));
        return dto;
    }
}
