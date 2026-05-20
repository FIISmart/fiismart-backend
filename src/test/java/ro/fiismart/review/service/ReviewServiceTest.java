package ro.fiismart.review.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Review;
import ro.fiismart.common.repository.ReviewRepository;
import ro.fiismart.review.dto.ReviewRequest;
import ro.fiismart.review.dto.ReviewResponse;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private Review sampleReview;

    @BeforeEach
    void setUp() {
        sampleReview = Review.builder()
                .id("rev1")
                .studentId("student1")
                .courseId("course1")
                .stars(4)
                .body("Super curs!")
                .createdAt(new Date())
                .deleted(false)
                .build();
    }

    @Test
    void create_savesAndReturnsResponse() {
        ReviewRequest req = new ReviewRequest();
        req.setStudentId("student1");
        req.setCourseId("course1");
        req.setStars(5);
        req.setBody("Excelent!");

        when(reviewRepository.save(any(Review.class))).thenReturn(sampleReview);

        ReviewResponse result = reviewService.create(req);

        assertThat(result.getId()).isEqualTo("rev1");
        assertThat(result.getStars()).isEqualTo(4);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void findById_found_returnsResponse() {
        when(reviewRepository.findById("rev1")).thenReturn(Optional.of(sampleReview));

        ReviewResponse result = reviewService.findById("rev1");

        assertThat(result.getId()).isEqualTo("rev1");
    }

    @Test
    void findById_notFound_throws() {
        when(reviewRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.findById("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByStudentAndCourse_found_returnsResponse() {
        when(reviewRepository.findByStudentIdAndCourseIdAndDeletedFalse("student1", "course1"))
                .thenReturn(Optional.of(sampleReview));

        ReviewResponse result = reviewService.findByStudentAndCourse("student1", "course1");

        assertThat(result.getStudentId()).isEqualTo("student1");
    }

    @Test
    void findByStudentAndCourse_notFound_throws() {
        when(reviewRepository.findByStudentIdAndCourseIdAndDeletedFalse("s", "c"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.findByStudentAndCourse("s", "c"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByCourseId_returnsMappedList() {
        when(reviewRepository.findByCourseIdAndDeletedFalse("course1"))
                .thenReturn(List.of(sampleReview));

        List<ReviewResponse> result = reviewService.findByCourseId("course1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseId()).isEqualTo("course1");
    }

    @Test
    void findByStudentId_returnsMappedList() {
        when(reviewRepository.findByStudentIdAndDeletedFalse("student1"))
                .thenReturn(List.of(sampleReview));

        List<ReviewResponse> result = reviewService.findByStudentId("student1");

        assertThat(result).hasSize(1);
    }

    @Test
    void findByCourseAndStars_returnsMappedList() {
        when(reviewRepository.findByCourseIdAndStarsAndDeletedFalse("course1", 5))
                .thenReturn(List.of());

        List<ReviewResponse> result = reviewService.findByCourseAndStars("course1", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void computeAvgRating_emptyList_returnsZero() {
        when(reviewRepository.findByCourseIdAndDeletedFalse("course1")).thenReturn(List.of());

        double avg = reviewService.computeAvgRating("course1");

        assertThat(avg).isEqualTo(0.0);
    }

    @Test
    void computeAvgRating_withReviews_returnsCorrectAvg() {
        Review r2 = Review.builder().stars(2).build();
        Review r4 = Review.builder().stars(4).build();
        when(reviewRepository.findByCourseIdAndDeletedFalse("course1"))
                .thenReturn(List.of(r2, r4));

        double avg = reviewService.computeAvgRating("course1");

        assertThat(avg).isEqualTo(3.0);
    }

    @Test
    void computeAvgRating_roundsToOneDecimal() {
        Review r1 = Review.builder().stars(5).build();
        Review r2 = Review.builder().stars(4).build();
        Review r3 = Review.builder().stars(3).build();
        when(reviewRepository.findByCourseIdAndDeletedFalse("course1"))
                .thenReturn(List.of(r1, r2, r3));

        double avg = reviewService.computeAvgRating("course1");

        assertThat(avg).isEqualTo(4.0);
    }

    @Test
    void updateReview_callsMongoTemplate() {
        reviewService.updateReview("rev1", 3, "Decent");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Review.class));
    }

    @Test
    void softDelete_callsMongoTemplate() {
        reviewService.softDelete("rev1", "admin1");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Review.class));
    }

    @Test
    void deleteById_callsRepository() {
        reviewService.deleteById("rev1");

        verify(reviewRepository).deleteById("rev1");
    }

    @Test
    void hasStudentReviewedCourse_delegatesToRepository() {
        when(reviewRepository.existsByStudentIdAndCourseIdAndDeletedFalse("s", "c")).thenReturn(true);

        assertThat(reviewService.hasStudentReviewedCourse("s", "c")).isTrue();
    }

    @Test
    void countByCourse_delegatesToRepository() {
        when(reviewRepository.countByCourseIdAndDeletedFalse("course1")).thenReturn(7L);

        assertThat(reviewService.countByCourse("course1")).isEqualTo(7L);
    }
}
