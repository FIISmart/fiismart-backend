package com.fiismart.backend.service.student;

import com.fiismart.backend.dto.student.CommentCreateRequest;
import com.fiismart.backend.dto.student.StudentCommentDTO;
import database.dao.CommentDAO;
import database.dao.UserDAO;
import database.model.Comment;
import database.model.User;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StudentCommentService {

    private final CommentDAO commentDAO;
    private final UserDAO userDAO;

    public StudentCommentService() {
        this.commentDAO = new CommentDAO();
        this.userDAO = new UserDAO();
    }

    public List<StudentCommentDTO> getCommentsThreaded(String studentIdHex, String lectureIdHex) {
        ObjectId lectureId = new ObjectId(lectureIdHex);
        ObjectId studentId = new ObjectId(studentIdHex);

        List<Comment> allComments = commentDAO.findByLectureId(lectureId);

        Set<ObjectId> authorIds = allComments.stream().map(Comment::getAuthorId).collect(Collectors.toSet());
        Map<ObjectId, User> authorsMap = new HashMap<>();
        for (ObjectId id : authorIds) {
            User u = userDAO.findById(id);
            if (u != null) authorsMap.put(id, u);
        }

        Map<ObjectId, StudentCommentDTO> dtoMap = new LinkedHashMap<>();
        for (Comment c : allComments) {
            dtoMap.put(c.getId(), convertToDTO(c, studentId, authorsMap.get(c.getAuthorId())));
        }

        List<StudentCommentDTO> topLevelComments = new ArrayList<>();
        for (Comment c : allComments) {
            StudentCommentDTO dto = dtoMap.get(c.getId());
            if (c.getParentCommentId() == null) {
                topLevelComments.add(dto);
            } else {
                StudentCommentDTO parent = dtoMap.get(c.getParentCommentId());
                if (parent != null) {
                    parent.getReplies().add(dto);
                }
            }
        }

        topLevelComments.sort((a, b) -> Integer.compare(b.getLikeCount(), a.getLikeCount()));

        return topLevelComments;
    }

    public StudentCommentDTO createComment(String studentIdHex, String courseIdHex, String lectureIdHex, CommentCreateRequest request) {
        Comment comment = Comment.builder()
                .authorId(new ObjectId(studentIdHex))
                .courseId(new ObjectId(courseIdHex))
                .lectureId(new ObjectId(lectureIdHex))
                .body(request.getBody())
                .videoTimestamp(request.getVideoTimestamp())
                .createdAt(new Date())
                .isDeleted(false)
                .likeCount(0)
                .build();

        ObjectId generatedId = commentDAO.insert(comment);
        comment.setId(generatedId);

        return convertToDTO(comment, new ObjectId(studentIdHex), userDAO.findById(comment.getAuthorId()));
    }

    public StudentCommentDTO replyToComment(String studentIdHex, String parentCommentIdHex, CommentCreateRequest request) {
        Comment parent = commentDAO.findById(new ObjectId(parentCommentIdHex));
        if (parent == null) throw new RuntimeException("Parent comment not found");

        Comment reply = Comment.builder()
                .authorId(new ObjectId(studentIdHex))
                .courseId(parent.getCourseId())
                .lectureId(parent.getLectureId())
                .parentCommentId(parent.getId())
                .body(request.getBody())
                .createdAt(new Date())
                .isDeleted(false)
                .likeCount(0)
                .build();

        ObjectId generatedId = commentDAO.insert(reply);
        reply.setId(generatedId);

        return convertToDTO(reply, new ObjectId(studentIdHex), userDAO.findById(reply.getAuthorId()));
    }

    public void toggleLike(String studentIdHex, String commentIdHex) {
        ObjectId commentId = new ObjectId(commentIdHex);
        ObjectId studentId = new ObjectId(studentIdHex);

        if (commentDAO.hasUserLiked(commentId, studentId)) {
            commentDAO.removeLike(commentId, studentId);
        } else {
            commentDAO.addLike(commentId, studentId);
        }
    }


    private StudentCommentDTO convertToDTO(Comment comment, ObjectId currentStudentId, User author) {
        String roleLabel = "Elev"; // Default
        String authorName = "Utilizator necunoscut";

        if (author != null) {
            authorName = author.getDisplayName() != null ? author.getDisplayName() : "Fără nume";
            if ("teacher".equals(author.getRole())) {
                roleLabel = "Profesor";
            } else if ("admin".equals(author.getRole())) {
                roleLabel = "Admin";
            }
        }

        String body = comment.getBody() != null ? comment.getBody() : "";
        int likes = comment.getLikeCount();

        boolean isLiked = false;
        if (comment.getLikedBy() != null && currentStudentId != null) {
            isLiked = comment.getLikedBy().contains(currentStudentId);
        }

        return StudentCommentDTO.builder()
                .commentId(comment.getId() != null ? comment.getId().toHexString() : "")
                .authorName(authorName)
                .authorRole(roleLabel)
                .body(body)
                .videoTimestamp(comment.getVideoTimestamp())
                .likeCount(likes)
                .timeAgo(getTimeAgo(comment.getCreatedAt()))
                .isLikedByMe(isLiked)
                .replies(new ArrayList<>())
                .build();
    }

    private String getTimeAgo(Date past) {
        if (past == null) return "Acum ceva timp";
        long seconds = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime() - past.getTime());
        if (seconds < 60) return "Acum cateva secunde";
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        if (minutes < 60) return "Acum " + minutes + " minute";
        long hours = TimeUnit.MINUTES.toHours(minutes);
        if (hours < 24) return "Acum " + hours + (hours == 1 ? " ora" : " ore");
        long days = TimeUnit.HOURS.toDays(hours);
        return "Acum " + days + (days == 1 ? " zi" : " zile");
    }
}