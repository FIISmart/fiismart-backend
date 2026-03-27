package com.fiismart.backend.course.service;

import com.fiismart.backend.course.dto.request.CreateLectureRequest;
import com.fiismart.backend.course.dto.request.UpdateLectureRequest;
import com.fiismart.backend.course.dto.response.LectureResponse;
import com.fiismart.backend.course.exception.BadRequestException;
import com.fiismart.backend.course.exception.ResourceNotFoundException;
import database.dao.CourseDAO;
import database.model.Course;
import database.model.Lecture;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LectureService {

    private final CourseDAO courseDAO;

    public LectureService(CourseDAO courseDAO) {
        this.courseDAO = courseDAO;
    }

    public LectureResponse addLecture(String courseId, CreateLectureRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        Course course = courseDAO.findById(cid);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }

        Lecture lecture = Lecture.builder()
                .id(new ObjectId())
                .title(req.getTitle())
                .videoUrl(req.getVideoUrl())
                .imageUrls(req.getImageUrls() != null ? req.getImageUrls() : new ArrayList<>())
                .order(req.getOrder())
                .durationSecs(req.getDurationSecs())
                .publishedAt(new Date())
                .build();

        courseDAO.addLecture(cid, lecture);
        courseDAO.updateUpdatedAt(cid, new Date());
        return LectureResponse.fromModel(lecture);
    }

    public LectureResponse updateLecture(String courseId, String lectureId, UpdateLectureRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId lid = toObjectId(lectureId, "Invalid lecture ID");

        Course course = courseDAO.findById(cid);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }

        boolean lectureExists = course.getLectures().stream()
                .anyMatch(l -> l.getId().equals(lid));
        if (!lectureExists) {
            throw new ResourceNotFoundException("Lecture not found: " + lectureId);
        }

        if (req.getTitle() != null) {
            courseDAO.updateLectureField(cid, lid, "title", req.getTitle());
        }
        if (req.getVideoUrl() != null) {
            courseDAO.updateLectureField(cid, lid, "videoUrl", req.getVideoUrl());
        }
        if (req.getImageUrls() != null) {
            courseDAO.updateLectureField(cid, lid, "imageUrls", req.getImageUrls());
        }
        if (req.getOrder() != null) {
            courseDAO.updateLectureField(cid, lid, "order", req.getOrder());
        }
        if (req.getDurationSecs() != null) {
            courseDAO.updateLectureField(cid, lid, "durationSecs", req.getDurationSecs());
        }

        courseDAO.updateUpdatedAt(cid, new Date());

        Course updated = courseDAO.findById(cid);
        Lecture updatedLecture = updated.getLectures().stream()
                .filter(l -> l.getId().equals(lid))
                .findFirst()
                .orElse(null);
        return LectureResponse.fromModel(updatedLecture);
    }

    public void deleteLecture(String courseId, String lectureId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId lid = toObjectId(lectureId, "Invalid lecture ID");

        if (!courseDAO.existsById(cid)) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }

        courseDAO.removeLecture(cid, lid);
        courseDAO.updateUpdatedAt(cid, new Date());
    }

    public List<LectureResponse> getLectures(String courseId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        return courseDAO.findLecturesByCourseId(cid).stream()
                .map(LectureResponse::fromModel)
                .collect(Collectors.toList());
    }

    private ObjectId toObjectId(String id, String errorMessage) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException(errorMessage + ": " + id);
        }
        return new ObjectId(id);
    }
}
