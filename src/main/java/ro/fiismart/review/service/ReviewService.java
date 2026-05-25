package ro.fiismart.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Review;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.ReviewRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.review.dto.ReviewRequest;
import ro.fiismart.review.dto.ReviewResponse;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;

    public ReviewResponse create(ReviewRequest request) {
        Review review = Review.builder()
                .studentId(request.getStudentId())
                .courseId(request.getCourseId())
                .stars(request.getStars())
                .body(request.getBody())
                .createdAt(new Date())
                .deleted(false)
                .build();
        return toResponse(reviewRepository.save(review));
    }

    public ReviewResponse findById(String reviewId) {
        return toResponse(reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId)));
    }

    public ReviewResponse findByStudentAndCourse(String studentId, String courseId) {
        return toResponse(reviewRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found for student " + studentId + " and course " + courseId)));
    }

    public List<ReviewResponse> findByCourseId(String courseId) {
        return reviewRepository.findByCourseIdAndDeletedFalse(courseId)
                .stream().map(this::toResponse).toList();
    }

    public List<ReviewResponse> findByStudentId(String studentId) {
        return reviewRepository.findByStudentIdAndDeletedFalse(studentId)
                .stream().map(this::toResponse).toList();
    }

    public List<ReviewResponse> findByCourseAndStars(String courseId, int stars) {
        return reviewRepository.findByCourseIdAndStarsAndDeletedFalse(courseId, stars)
                .stream().map(this::toResponse).toList();
    }

    public double computeAvgRating(String courseId) {
        List<Review> reviews = reviewRepository.findByCourseIdAndDeletedFalse(courseId);
        if (reviews.isEmpty()) return 0.0;
        double total = reviews.stream().mapToInt(Review::getStars).sum();
        return Math.round((total / reviews.size()) * 10.0) / 10.0;
    }

    public void updateReview(String reviewId, int newStars, String newBody) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(reviewId)),
                new Update().set("stars", newStars).set("body", newBody),
                Review.class);
    }

    public void softDelete(String reviewId, String deletedByUserId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(reviewId)),
                new Update().set("isDeleted", true).set("deletedBy", deletedByUserId),
                Review.class);
    }

    public void deleteById(String reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    public boolean hasStudentReviewedCourse(String studentId, String courseId) {
        return reviewRepository.existsByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId);
    }

    public long countByCourse(String courseId) {
        return reviewRepository.countByCourseIdAndDeletedFalse(courseId);
    }

    private ReviewResponse toResponse(Review r) {
        String authorName = userRepository.findById(r.getStudentId())
                .map(User::getDisplayName)
                .orElse("Utilizator necunoscut");
        return ReviewResponse.builder()
                .id(r.getId())
                .studentId(r.getStudentId())
                .authorName(authorName)
                .courseId(r.getCourseId())
                .stars(r.getStars())
                .body(r.getBody())
                .createdAt(r.getCreatedAt())
                .deleted(r.isDeleted())
                .deletedBy(r.getDeletedBy())
                .build();
    }
}
