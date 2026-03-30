package com.fiismart.teacher_dashboard.service;

import com.fiismart.teacher_dashboard.dto.TeacherCommentPreviewDTO;
import database.dao.CommentDAO;
import database.dao.CourseDAO;
import database.dao.UserDAO;
import database.model.Comment;
import database.model.Course;
import database.model.User;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TeacherCommentsService {
    private final UserDAO userDAO = new UserDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final CommentDAO commentDAO = new CommentDAO();

    public List<TeacherCommentPreviewDTO> getComments(String teacherIdHex, int limit, int offset) {
        ObjectId teacherId = new ObjectId(teacherIdHex);
        List<Course> courses = courseDAO.findByTeacherId(teacherId);

        Map<ObjectId, String> courseTitles = new HashMap<>();
        List<ObjectId> lectureIds = new ArrayList<>();
        for (Course c : courses) {
            courseTitles.put(c.getId(), c.getTitle());
            if (c.getLectures() != null) {
                c.getLectures().forEach(l -> lectureIds.add(l.getId()));
            }
        }

        List<TeacherCommentPreviewDTO> result = new ArrayList<>();
        for (ObjectId lectureId : lectureIds) {
            List<Comment> comments = commentDAO.findTopLevelByLectureId(lectureId);
            for (Comment comment : comments) {
                List<Comment> replies = commentDAO.findRepliesByParentId(comment.getId());
                User author = userDAO.findById(comment.getAuthorId());

                TeacherCommentPreviewDTO dto = new TeacherCommentPreviewDTO();
                dto.setCommentId(comment.getId().toHexString());
                dto.setCourseId(comment.getCourseId() != null ? comment.getCourseId().toHexString() : null);
                dto.setCourseTitle(courseTitles.get(comment.getCourseId()));
                dto.setLectureId(comment.getLectureId() != null ? comment.getLectureId().toHexString() : null);
                dto.setAuthorId(comment.getAuthorId() != null ? comment.getAuthorId().toHexString() : null);
                dto.setAuthorDisplayName(author != null ? author.getDisplayName() : "");
                dto.setBody(comment.getBody());
                dto.setCreatedAt(comment.getCreatedAt());
                dto.setLikeCount(comment.getLikeCount());
                dto.setRepliesCount(replies.size());
                dto.setAnswered(!replies.isEmpty());
                result.add(dto);
            }
        }

        return result.stream()
                .skip(offset)
                .limit(limit)
                .toList();
    }


}
