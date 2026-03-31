package com.app.service;

import com.app.dto.request.ReviewRequest;
import com.app.dto.response.ReviewResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Review;
import com.app.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ReviewService reviewService;

    private Review buildReview(String id, String studentId, String courseId, int stars) {
        return Review.builder()
                .id(id)
                .studentId(studentId)
                .courseId(courseId)
                .stars(stars)
                .body("Good course")
                .createdAt(new Date())
                .deleted(false)
                .build();
    }

    @Test
    void create_savesReviewAndReturnsResponse() {
        ReviewRequest request = ReviewRequest.builder()
                .studentId("s1")
                .courseId("c1")
                .stars(5)
                .body("Excellent!")
                .build();

        Review saved = buildReview("r1", "s1", "c1", 5);
        saved.setBody("Excellent!");
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewResponse response = reviewService.create(request);

        assertThat(response.getId()).isEqualTo("r1");
        assertThat(response.getStudentId()).isEqualTo("s1");
        assertThat(response.getCourseId()).isEqualTo("c1");
        assertThat(response.getStars()).isEqualTo(5);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void create_setsInitialValues() {
        ReviewRequest request = ReviewRequest.builder()
                .studentId("s1")
                .courseId("c1")
                .stars(4)
                .body("Nice")
                .build();

        Review saved = buildReview("r1", "s1", "c1", 4);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        reviewService.create(request);
        verify(reviewRepository).save(captor.capture());

        assertThat(captor.getValue().isDeleted()).isFalse();
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void create_withNullBody_setsNullBody() {
        ReviewRequest request = ReviewRequest.builder()
                .studentId("s1")
                .courseId("c1")
                .stars(3)
                .body(null)
                .build();

        Review saved = buildReview("r1", "s1", "c1", 3);
        saved.setBody(null);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        reviewService.create(request);
        verify(reviewRepository).save(captor.capture());

        assertThat(captor.getValue().getBody()).isNull();
    }

    @Test
    void findById_returnsResponse_whenFound() {
        Review review = buildReview("r1", "s1", "c1", 4);
        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.findById("r1");

        assertThat(response.getId()).isEqualTo("r1");
        assertThat(response.getStars()).isEqualTo(4);
    }

    @Test
    void findById_throwsResourceNotFoundException_whenNotFound() {
        when(reviewRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review")
                .hasMessageContaining("missing");
    }

    @Test
    void findByStudentAndCourse_returnsResponse_whenFound() {
        Review review = buildReview("r1", "s1", "c1", 5);
        when(reviewRepository.findByStudentIdAndCourseIdAndDeletedFalse("s1", "c1"))
                .thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.findByStudentAndCourse("s1", "c1");

        assertThat(response.getStudentId()).isEqualTo("s1");
        assertThat(response.getCourseId()).isEqualTo("c1");
    }

    @Test
    void findByStudentAndCourse_throwsResourceNotFoundException_whenNotFound() {
        when(reviewRepository.findByStudentIdAndCourseIdAndDeletedFalse("s1", "c1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.findByStudentAndCourse("s1", "c1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("s1")
                .hasMessageContaining("c1");
    }

    @Test
    void findByCourseId_returnsNonDeletedReviews() {
        List<Review> reviews = List.of(
                buildReview("r1", "s1", "c1", 5),
                buildReview("r2", "s2", "c1", 3)
        );
        when(reviewRepository.findByCourseIdAndDeletedFalse("c1")).thenReturn(reviews);

        List<ReviewResponse> responses = reviewService.findByCourseId("c1");

        assertThat(responses).hasSize(2);
    }

    @Test
    void findByCourseId_returnsEmptyList_whenNone() {
        when(reviewRepository.findByCourseIdAndDeletedFalse("c99")).thenReturn(List.of());

        assertThat(reviewService.findByCourseId("c99")).isEmpty();
    }

    @Test
    void findByStudentId_returnsNonDeletedReviews() {
        List<Review> reviews = List.of(
                buildReview("r1", "s1", "c1", 4),
                buildReview("r2", "s1", "c2", 5)
        );
        when(reviewRepository.findByStudentIdAndDeletedFalse("s1")).thenReturn(reviews);

        List<ReviewResponse> responses = reviewService.findByStudentId("s1");

        assertThat(responses).hasSize(2);
    }

    @Test
    void findByCourseAndStars_returnsFilteredReviews() {
        List<Review> reviews = List.of(buildReview("r1", "s1", "c1", 5));
        when(reviewRepository.findByCourseIdAndStarsAndDeletedFalse("c1", 5)).thenReturn(reviews);

        List<ReviewResponse> responses = reviewService.findByCourseAndStars("c1", 5);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStars()).isEqualTo(5);
    }

    @Test
    void findByCourseAndStars_returnsEmptyList_whenNoMatch() {
        when(reviewRepository.findByCourseIdAndStarsAndDeletedFalse("c1", 1)).thenReturn(List.of());

        assertThat(reviewService.findByCourseAndStars("c1", 1)).isEmpty();
    }

    @Test
    void computeAvgRating_returnsZero_whenNoReviews() {
        when(reviewRepository.findByCourseIdAndDeletedFalse("c1")).thenReturn(List.of());

        double avg = reviewService.computeAvgRating("c1");

        assertThat(avg).isEqualTo(0.0);
    }

    @Test
    void computeAvgRating_returnsSingleRating_whenOneReview() {
        when(reviewRepository.findByCourseIdAndDeletedFalse("c1"))
                .thenReturn(List.of(buildReview("r1", "s1", "c1", 4)));

        double avg = reviewService.computeAvgRating("c1");

        assertThat(avg).isEqualTo(4.0);
    }

    @Test
    void computeAvgRating_returnsAverage_whenMultipleReviews() {
        List<Review> reviews = List.of(
                buildReview("r1", "s1", "c1", 4),
                buildReview("r2", "s2", "c1", 2)
        );
        when(reviewRepository.findByCourseIdAndDeletedFalse("c1")).thenReturn(reviews);

        double avg = reviewService.computeAvgRating("c1");

        assertThat(avg).isEqualTo(3.0);
    }

    @Test
    void computeAvgRating_roundsToOneDecimalPlace() {
        List<Review> reviews = List.of(
                buildReview("r1", "s1", "c1", 5),
                buildReview("r2", "s2", "c1", 5),
                buildReview("r3", "s3", "c1", 4)
        );
        when(reviewRepository.findByCourseIdAndDeletedFalse("c1")).thenReturn(reviews);

        double avg = reviewService.computeAvgRating("c1");

        assertThat(avg).isEqualTo(4.7);
    }

    @Test
    void computeAvgRating_handlesNonRoundFraction() {
        List<Review> reviews = List.of(
                buildReview("r1", "s1", "c1", 1),
                buildReview("r2", "s2", "c1", 1),
                buildReview("r3", "s3", "c1", 2)
        );
        when(reviewRepository.findByCourseIdAndDeletedFalse("c1")).thenReturn(reviews);

        double avg = reviewService.computeAvgRating("c1");

        assertThat(avg).isEqualTo(1.3);
    }

    @Test
    void computeAvgRating_withAllFiveStars_returns5() {
        List<Review> reviews = List.of(
                buildReview("r1", "s1", "c1", 5),
                buildReview("r2", "s2", "c1", 5),
                buildReview("r3", "s3", "c1", 5)
        );
        when(reviewRepository.findByCourseIdAndDeletedFalse("c1")).thenReturn(reviews);

        double avg = reviewService.computeAvgRating("c1");

        assertThat(avg).isEqualTo(5.0);
    }

    @Test
    void updateReview_invokesMongoTemplate() {
        reviewService.updateReview("r1", 4, "Updated body");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Review.class));
    }

    @Test
    void softDelete_invokesMongoTemplate() {
        reviewService.softDelete("r1", "adminId");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Review.class));
    }

    @Test
    void deleteById_delegatesToRepository() {
        reviewService.deleteById("r1");

        verify(reviewRepository).deleteById("r1");
    }

    @Test
    void deleteAllByCourse_delegatesToRepository() {
        reviewService.deleteAllByCourse("c1");

        verify(reviewRepository).deleteByCourseId("c1");
    }

    @Test
    void hasStudentReviewedCourse_returnsTrue_whenReviewed() {
        when(reviewRepository.existsByStudentIdAndCourseIdAndDeletedFalse("s1", "c1")).thenReturn(true);

        assertThat(reviewService.hasStudentReviewedCourse("s1", "c1")).isTrue();
    }

    @Test
    void hasStudentReviewedCourse_returnsFalse_whenNotReviewed() {
        when(reviewRepository.existsByStudentIdAndCourseIdAndDeletedFalse("s1", "c1")).thenReturn(false);

        assertThat(reviewService.hasStudentReviewedCourse("s1", "c1")).isFalse();
    }

    @Test
    void countByCourse_returnsCount() {
        when(reviewRepository.countByCourseIdAndDeletedFalse("c1")).thenReturn(20L);

        assertThat(reviewService.countByCourse("c1")).isEqualTo(20L);
    }

    @Test
    void countByCourse_returnsZero_whenNone() {
        when(reviewRepository.countByCourseIdAndDeletedFalse("c99")).thenReturn(0L);

        assertThat(reviewService.countByCourse("c99")).isEqualTo(0L);
    }

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        Date createdAt = new Date();
        Review review = Review.builder()
                .id("r1")
                .studentId("s1")
                .courseId("c1")
                .stars(5)
                .body("Fantastic course!")
                .createdAt(createdAt)
                .deleted(false)
                .deletedBy(null)
                .build();

        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.findById("r1");

        assertThat(response.getId()).isEqualTo("r1");
        assertThat(response.getStudentId()).isEqualTo("s1");
        assertThat(response.getCourseId()).isEqualTo("c1");
        assertThat(response.getStars()).isEqualTo(5);
        assertThat(response.getBody()).isEqualTo("Fantastic course!");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.isDeleted()).isFalse();
        assertThat(response.getDeletedBy()).isNull();
    }

    @Test
    void toResponse_withDeletedReview_mapsDeletedByField() {
        Review review = Review.builder()
                .id("r1")
                .studentId("s1")
                .courseId("c1")
                .stars(2)
                .body("Bad")
                .createdAt(new Date())
                .deleted(true)
                .deletedBy("adminId")
                .build();

        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.findById("r1");

        assertThat(response.isDeleted()).isTrue();
        assertThat(response.getDeletedBy()).isEqualTo("adminId");
    }
}
