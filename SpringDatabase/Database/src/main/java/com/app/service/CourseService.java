package com.app.service;

import com.app.dto.request.CourseRequest;
import com.app.dto.request.LectureRequest;
import com.app.dto.response.CourseResponse;
import com.app.dto.response.LectureResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Course;
import com.app.model.Lecture;
import com.app.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final MongoTemplate mongoTemplate;

    public CourseResponse create(CourseRequest request) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .teacherId(request.getTeacherId())
                .status(request.getStatus() != null ? request.getStatus() : "draft")
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .thumbnailUrl(request.getThumbnailUrl())
                .language(request.getLanguage())
                .enrollmentCount(0)
                .avgRating(0.0)
                .hidden(false)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        return toResponse(courseRepository.save(course));
    }

    public CourseResponse findById(String id) {
        return toResponse(courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id)));
    }

    public List<CourseResponse> findAll() {
        return courseRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<CourseResponse> findByTeacherId(String teacherId) {
        return courseRepository.findByTeacherId(teacherId).stream().map(this::toResponse).toList();
    }

    public List<CourseResponse> findPublishedVisible() {
        return courseRepository.findByStatusAndHidden("published", false)
                .stream().map(this::toResponse).toList();
    }

    public List<CourseResponse> findByTag(String tag) {
        return courseRepository.findByTagsContaining(tag).stream().map(this::toResponse).toList();
    }

    public List<CourseResponse> findByMinRating(double minRating) {
        return courseRepository.findByAvgRatingGreaterThanEqual(minRating)
                .stream().map(this::toResponse).toList();
    }

    public List<LectureResponse> findLecturesByCourseId(String courseId) {
        return courseRepository.findById(courseId)
                .map(c -> c.getLectures().stream().map(this::toLectureResponse).toList())
                .orElse(new ArrayList<>());
    }

    public void updateTitle(String courseId, String newTitle) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId)),
                new Update().set("title", newTitle).set("updatedAt", new Date()),
                Course.class);
    }

    public void updateDescription(String courseId, String description) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId)),
                new Update().set("description", description).set("updatedAt", new Date()),
                Course.class);
    }

    public void updateStatus(String courseId, String status) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId)),
                new Update().set("status", status).set("updatedAt", new Date()),
                Course.class);
    }

    public void setHidden(String courseId, boolean hidden) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId)),
                new Update().set("isHidden", hidden).set("updatedAt", new Date()),
                Course.class);
    }

    public void setQuizId(String courseId, String quizId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId)),
                new Update().set("quizId", quizId),
                Course.class);
    }

    public void incrementEnrollmentCount(String courseId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId)),
                new Update().inc("enrollmentCount", 1),
                Course.class);
    }

    public void decrementEnrollmentCount(String courseId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId)),
                new Update().inc("enrollmentCount", -1),
                Course.class);
    }

    public void updateAvgRating(String courseId, double avgRating) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId)),
                new Update().set("avgRating", avgRating),
                Course.class);
    }

    public LectureResponse addLecture(String courseId, LectureRequest request) {
        Lecture lecture = Lecture.builder()
                .id(new ObjectId().toHexString())
                .title(request.getTitle())
                .videoUrl(request.getVideoUrl())
                .imageUrls(request.getImageUrls() != null ? request.getImageUrls() : new ArrayList<>())
                .order(request.getOrder())
                .durationSecs(request.getDurationSecs())
                .publishedAt(new Date())
                .build();
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId)),
                new Update().push("lectures", lecture),
                Course.class);
        return toLectureResponse(lecture);
    }

    public void removeLecture(String courseId, String lectureId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId)),
                new Update().pull("lectures", new org.bson.Document("id", lectureId)),
                Course.class);
    }

    public void updateLectureField(String courseId, String lectureId, String field, Object value) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(courseId).and("lectures.id").is(lectureId)),
                new Update().set("lectures.$." + field, value),
                Course.class);
    }

    public void deleteById(String courseId) {
        courseRepository.deleteById(courseId);
    }

    public boolean existsById(String courseId) {
        return courseRepository.existsById(courseId);
    }

    public long countByTeacher(String teacherId) {
        return courseRepository.countByTeacherId(teacherId);
    }

    private CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .teacherId(course.getTeacherId())
                .status(course.getStatus())
                .tags(course.getTags())
                .thumbnailUrl(course.getThumbnailUrl())
                .language(course.getLanguage())
                .enrollmentCount(course.getEnrollmentCount())
                .avgRating(course.getAvgRating())
                .lectures(course.getLectures() != null
                        ? course.getLectures().stream().map(this::toLectureResponse).toList()
                        : new ArrayList<>())
                .hidden(course.isHidden())
                .quizId(course.getQuizId())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private LectureResponse toLectureResponse(Lecture lecture) {
        return LectureResponse.builder()
                .id(lecture.getId())
                .title(lecture.getTitle())
                .videoUrl(lecture.getVideoUrl())
                .imageUrls(lecture.getImageUrls())
                .order(lecture.getOrder())
                .durationSecs(lecture.getDurationSecs())
                .publishedAt(lecture.getPublishedAt())
                .build();
    }
}
