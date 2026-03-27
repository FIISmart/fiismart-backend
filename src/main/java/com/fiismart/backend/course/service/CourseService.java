package com.fiismart.backend.course.service;

import com.fiismart.backend.course.dto.request.CreateCourseRequest;
import com.fiismart.backend.course.dto.request.UpdateCourseRequest;
import com.fiismart.backend.course.dto.response.CourseResponse;
import com.fiismart.backend.course.exception.BadRequestException;
import com.fiismart.backend.course.exception.ResourceNotFoundException;
import com.fiismart.backend.course.helper.CourseUpdateHelper;
import database.dao.CourseDAO;
import database.model.Course;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseDAO courseDAO;
    private final CourseUpdateHelper courseUpdateHelper;

    public CourseService(CourseDAO courseDAO, CourseUpdateHelper courseUpdateHelper) {
        this.courseDAO = courseDAO;
        this.courseUpdateHelper = courseUpdateHelper;
    }

    public CourseResponse createCourse(CreateCourseRequest req) {
        ObjectId teacherId = toObjectId(req.getTeacherId(), "Invalid teacher ID");

        Course course = Course.builder()
                .id(new ObjectId())
                .title(req.getTitle())
                .description(req.getDescription())
                .teacherId(teacherId)
                .status("draft")
                .tags(req.getTags() != null ? req.getTags() : new ArrayList<>())
                .thumbnailUrl(req.getThumbnailUrl())
                .language(req.getLanguage())
                .enrollmentCount(0)
                .avgRating(0.0)
                .lectures(new ArrayList<>())
                .isHidden(false)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        courseDAO.insert(course);
        return CourseResponse.fromModel(course);
    }

    public CourseResponse getCourseById(String id) {
        ObjectId courseId = toObjectId(id, "Invalid course ID");
        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found: " + id);
        }
        return CourseResponse.fromModel(course);
    }

    public List<CourseResponse> getCoursesByTeacherId(String teacherId) {
        ObjectId tid = toObjectId(teacherId, "Invalid teacher ID");
        return courseDAO.findByTeacherId(tid).stream()
                .map(CourseResponse::fromModel)
                .collect(Collectors.toList());
    }

    public List<CourseResponse> getPublishedCourses() {
        return courseDAO.findPublishedVisible().stream()
                .map(CourseResponse::fromModel)
                .collect(Collectors.toList());
    }

    public CourseResponse updateCourse(String id, UpdateCourseRequest req) {
        ObjectId courseId = toObjectId(id, "Invalid course ID");
        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found: " + id);
        }

        if (req.getTitle() != null) {
            courseDAO.updateTitle(courseId, req.getTitle());
        }
        if (req.getDescription() != null) {
            courseDAO.updateDescription(courseId, req.getDescription());
        }
        if (req.getTags() != null) {
            courseUpdateHelper.updateTags(courseId, req.getTags());
        }
        if (req.getThumbnailUrl() != null) {
            courseUpdateHelper.updateThumbnailUrl(courseId, req.getThumbnailUrl());
        }
        if (req.getLanguage() != null) {
            courseUpdateHelper.updateLanguage(courseId, req.getLanguage());
        }
        courseDAO.updateUpdatedAt(courseId, new Date());

        return CourseResponse.fromModel(courseDAO.findById(courseId));
    }

    public CourseResponse publishCourse(String id) {
        ObjectId courseId = toObjectId(id, "Invalid course ID");
        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found: " + id);
        }
        courseDAO.updateStatus(courseId, "published");
        courseDAO.updateUpdatedAt(courseId, new Date());
        return CourseResponse.fromModel(courseDAO.findById(courseId));
    }

    public CourseResponse draftCourse(String id) {
        ObjectId courseId = toObjectId(id, "Invalid course ID");
        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found: " + id);
        }
        courseDAO.updateStatus(courseId, "draft");
        courseDAO.updateUpdatedAt(courseId, new Date());
        return CourseResponse.fromModel(courseDAO.findById(courseId));
    }

    public void deleteCourse(String id) {
        ObjectId courseId = toObjectId(id, "Invalid course ID");
        if (!courseDAO.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found: " + id);
        }
        courseDAO.deleteById(courseId);
    }

    private ObjectId toObjectId(String id, String errorMessage) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException(errorMessage + ": " + id);
        }
        return new ObjectId(id);
    }
}
