package com.fiismart.backend.course.service;

import com.fiismart.backend.course.dto.response.CommentResponse;
import com.fiismart.backend.course.exception.BadRequestException;
import com.fiismart.backend.course.exception.ResourceNotFoundException;
import com.fiismart.backend.course.helper.CommentQueryHelper;
import database.dao.CommentDAO;
import database.model.Comment;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentDAO commentDAO;
    private final CommentQueryHelper commentQueryHelper;

    public CommentService(CommentDAO commentDAO, CommentQueryHelper commentQueryHelper) {
        this.commentDAO = commentDAO;
        this.commentQueryHelper = commentQueryHelper;
    }

    public List<CommentResponse> getCommentsByCourseId(String courseId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        // Include rejected (deleted) comments — the moderation UI has a Rejected tab.
        return commentQueryHelper.findAllByCourseId(cid).stream()
                .map(CommentResponse::fromModel)
                .collect(Collectors.toList());
    }

    public void updateCommentStatus(String commentId, String status) {
        ObjectId cid = toObjectId(commentId, "Invalid comment ID");
        Comment comment = commentDAO.findById(cid);
        if (comment == null) {
            throw new ResourceNotFoundException("Comment not found: " + commentId);
        }

        switch (status) {
            case "approved" -> commentQueryHelper.markApproved(cid);
            case "rejected" -> commentQueryHelper.markRejected(cid);
            case "pending" -> commentQueryHelper.markPending(cid);
            default -> throw new BadRequestException("Invalid status: " + status + ". Must be approved, rejected, or pending");
        }
    }

    public void deleteComment(String commentId) {
        ObjectId cid = toObjectId(commentId, "Invalid comment ID");
        Comment comment = commentDAO.findById(cid);
        if (comment == null) {
            throw new ResourceNotFoundException("Comment not found: " + commentId);
        }
        commentDAO.softDelete(cid);
    }

    private ObjectId toObjectId(String id, String errorMessage) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException(errorMessage + ": " + id);
        }
        return new ObjectId(id);
    }
}
