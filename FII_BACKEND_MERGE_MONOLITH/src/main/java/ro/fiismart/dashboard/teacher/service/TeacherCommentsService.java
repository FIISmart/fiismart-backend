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
                result.add(dto);
            }
        }

        return result.stream().skip(offset).limit(limit).toList();
    }
}
