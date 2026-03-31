package com.app.service;

import com.app.dto.request.CourseRequest;
import com.app.dto.request.LectureRequest;
import com.app.dto.response.CourseResponse;
import com.app.dto.response.LectureResponse;
import com.app.exception.ResourceNotFoundException;
import com.app.model.Course;
import com.app.model.Lecture;
import com.app.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private CourseService courseService;

    private Course buildCourse(String id, String title, String teacherId) {
        return Course.builder()
                .id(id)
                .title(title)
                .description("desc")
                .teacherId(teacherId)
                .status("draft")
                .tags(new ArrayList<>())
                .enrollmentCount(0)
                .avgRating(0.0)
                .lectures(new ArrayList<>())
                .hidden(false)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
    }

    @Test
    void create_withNullStatus_defaultsToDraft() {
        CourseRequest request = CourseRequest.builder()
                .title("Java 101")
                .teacherId("t1")
                .status(null)
                .build();

        Course saved = buildCourse("c1", "Java 101", "t1");
        when(courseRepository.save(any(Course.class))).thenReturn(saved);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        courseService.create(request);
        verify(courseRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo("draft");
    }

    @Test
    void create_withExplicitStatus_usesProvidedStatus() {
        CourseRequest request = CourseRequest.builder()
                .title("Java 101")
                .teacherId("t1")
                .status("published")
                .build();

        Course saved = buildCourse("c1", "Java 101", "t1");
        saved.setStatus("published");
        when(courseRepository.save(any(Course.class))).thenReturn(saved);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        courseService.create(request);
        verify(courseRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo("published");
    }

    @Test
    void create_withNullTags_initializesEmptyList() {
        CourseRequest request = CourseRequest.builder()
                .title("Course")
                .teacherId("t1")
                .tags(null)
                .build();

        Course saved = buildCourse("c1", "Course", "t1");
        when(courseRepository.save(any(Course.class))).thenReturn(saved);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        courseService.create(request);
        verify(courseRepository).save(captor.capture());

        assertThat(captor.getValue().getTags()).isNotNull().isEmpty();
    }

    @Test
    void create_withTags_preservesTags() {
        CourseRequest request = CourseRequest.builder()
                .title("Course")
                .teacherId("t1")
                .tags(List.of("java", "spring"))
                .build();

        Course saved = buildCourse("c1", "Course", "t1");
        when(courseRepository.save(any(Course.class))).thenReturn(saved);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        courseService.create(request);
        verify(courseRepository).save(captor.capture());

        assertThat(captor.getValue().getTags()).containsExactly("java", "spring");
    }

    @Test
    void create_setsInitialCountersAndDates() {
        CourseRequest request = CourseRequest.builder()
                .title("Course")
                .teacherId("t1")
                .build();

        Course saved = buildCourse("c1", "Course", "t1");
        when(courseRepository.save(any(Course.class))).thenReturn(saved);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        courseService.create(request);
        verify(courseRepository).save(captor.capture());

        assertThat(captor.getValue().getEnrollmentCount()).isEqualTo(0);
        assertThat(captor.getValue().getAvgRating()).isEqualTo(0.0);
        assertThat(captor.getValue().isHidden()).isFalse();
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void findById_returnsResponse_whenFound() {
        Course course = buildCourse("c1", "Java", "t1");
        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));

        CourseResponse response = courseService.findById("c1");

        assertThat(response.getId()).isEqualTo("c1");
        assertThat(response.getTitle()).isEqualTo("Java");
    }

    @Test
    void findById_throwsResourceNotFoundException_whenNotFound() {
        when(courseRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course")
                .hasMessageContaining("missing");
    }

    @Test
    void findAll_returnsAllCourses() {
        List<Course> courses = List.of(
                buildCourse("c1", "Course 1", "t1"),
                buildCourse("c2", "Course 2", "t2")
        );
        when(courseRepository.findAll()).thenReturn(courses);

        List<CourseResponse> responses = courseService.findAll();

        assertThat(responses).hasSize(2);
    }

    @Test
    void findAll_returnsEmptyList_whenNoCourses() {
        when(courseRepository.findAll()).thenReturn(List.of());

        assertThat(courseService.findAll()).isEmpty();
    }

    @Test
    void findByTeacherId_returnsCourses() {
        List<Course> courses = List.of(buildCourse("c1", "Course", "t1"));
        when(courseRepository.findByTeacherId("t1")).thenReturn(courses);

        List<CourseResponse> responses = courseService.findByTeacherId("t1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTeacherId()).isEqualTo("t1");
    }

    @Test
    void findPublishedVisible_returnsPublishedNotHiddenCourses() {
        Course course = buildCourse("c1", "Course", "t1");
        course.setStatus("published");
        when(courseRepository.findByStatusAndHidden("published", false)).thenReturn(List.of(course));

        List<CourseResponse> responses = courseService.findPublishedVisible();

        assertThat(responses).hasSize(1);
        verify(courseRepository).findByStatusAndHidden("published", false);
    }

    @Test
    void findByTag_returnsTaggedCourses() {
        Course course = buildCourse("c1", "Course", "t1");
        when(courseRepository.findByTagsContaining("java")).thenReturn(List.of(course));

        List<CourseResponse> responses = courseService.findByTag("java");

        assertThat(responses).hasSize(1);
    }

    @Test
    void findByMinRating_returnsCoursesMeetingThreshold() {
        Course course = buildCourse("c1", "Course", "t1");
        course.setAvgRating(4.5);
        when(courseRepository.findByAvgRatingGreaterThanEqual(4.0)).thenReturn(List.of(course));

        List<CourseResponse> responses = courseService.findByMinRating(4.0);

        assertThat(responses).hasSize(1);
    }

    @Test
    void findLecturesByCourseId_returnsLectures_whenCourseExists() {
        Lecture lecture = Lecture.builder()
                .id("lec1")
                .title("Intro")
                .videoUrl("url")
                .order(1)
                .durationSecs(300)
                .publishedAt(new Date())
                .build();

        Course course = buildCourse("c1", "Course", "t1");
        course.setLectures(List.of(lecture));
        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));

        List<LectureResponse> responses = courseService.findLecturesByCourseId("c1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo("lec1");
        assertThat(responses.get(0).getTitle()).isEqualTo("Intro");
    }

    @Test
    void findLecturesByCourseId_returnsEmptyList_whenCourseNotFound() {
        when(courseRepository.findById("missing")).thenReturn(Optional.empty());

        List<LectureResponse> responses = courseService.findLecturesByCourseId("missing");

        assertThat(responses).isEmpty();
    }

    @Test
    void findLecturesByCourseId_returnsEmptyList_whenCourseHasNoLectures() {
        Course course = buildCourse("c1", "Course", "t1");
        course.setLectures(new ArrayList<>());
        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));

        List<LectureResponse> responses = courseService.findLecturesByCourseId("c1");

        assertThat(responses).isEmpty();
    }

    @Test
    void updateTitle_invokesMongoTemplate() {
        courseService.updateTitle("c1", "New Title");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void updateDescription_invokesMongoTemplate() {
        courseService.updateDescription("c1", "New description");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void updateStatus_invokesMongoTemplate() {
        courseService.updateStatus("c1", "published");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void setHidden_true_invokesMongoTemplate() {
        courseService.setHidden("c1", true);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void setHidden_false_invokesMongoTemplate() {
        courseService.setHidden("c1", false);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void setQuizId_invokesMongoTemplate() {
        courseService.setQuizId("c1", "quiz1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void incrementEnrollmentCount_invokesMongoTemplate() {
        courseService.incrementEnrollmentCount("c1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void decrementEnrollmentCount_invokesMongoTemplate() {
        courseService.decrementEnrollmentCount("c1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void updateAvgRating_invokesMongoTemplate() {
        courseService.updateAvgRating("c1", 4.3);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void addLecture_withImageUrls_returnsLectureResponse() {
        LectureRequest request = LectureRequest.builder()
                .title("Lesson 1")
                .videoUrl("http://video.com/1")
                .imageUrls(List.of("img1.jpg"))
                .order(1)
                .durationSecs(600)
                .build();

        LectureResponse response = courseService.addLecture("c1", request);

        assertThat(response.getTitle()).isEqualTo("Lesson 1");
        assertThat(response.getVideoUrl()).isEqualTo("http://video.com/1");
        assertThat(response.getImageUrls()).containsExactly("img1.jpg");
        assertThat(response.getOrder()).isEqualTo(1);
        assertThat(response.getDurationSecs()).isEqualTo(600);
        assertThat(response.getId()).isNotNull();
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void addLecture_withNullImageUrls_initializesEmptyList() {
        LectureRequest request = LectureRequest.builder()
                .title("Lesson 2")
                .imageUrls(null)
                .order(2)
                .durationSecs(300)
                .build();

        LectureResponse response = courseService.addLecture("c1", request);

        assertThat(response.getImageUrls()).isNotNull().isEmpty();
    }

    @Test
    void addLecture_setsPublishedAt() {
        LectureRequest request = LectureRequest.builder()
                .title("Lesson")
                .order(1)
                .durationSecs(100)
                .build();

        LectureResponse response = courseService.addLecture("c1", request);

        assertThat(response.getPublishedAt()).isNotNull();
    }

    @Test
    void removeLecture_invokesMongoTemplate() {
        courseService.removeLecture("c1", "lec1");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void updateLectureField_invokesMongoTemplate() {
        courseService.updateLectureField("c1", "lec1", "title", "Updated Title");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Course.class));
    }

    @Test
    void deleteById_delegatesToRepository() {
        courseService.deleteById("c1");

        verify(courseRepository).deleteById("c1");
    }

    @Test
    void existsById_returnsTrue_whenExists() {
        when(courseRepository.existsById("c1")).thenReturn(true);

        assertThat(courseService.existsById("c1")).isTrue();
    }

    @Test
    void existsById_returnsFalse_whenNotExists() {
        when(courseRepository.existsById("missing")).thenReturn(false);

        assertThat(courseService.existsById("missing")).isFalse();
    }

    @Test
    void countByTeacher_returnsCount() {
        when(courseRepository.countByTeacherId("t1")).thenReturn(5L);

        assertThat(courseService.countByTeacher("t1")).isEqualTo(5L);
    }

    @Test
    void toResponse_withNullLectures_returnsEmptyLecturesList() {
        Course course = Course.builder()
                .id("c1")
                .title("Course")
                .teacherId("t1")
                .status("draft")
                .tags(new ArrayList<>())
                .lectures(null)
                .enrollmentCount(0)
                .avgRating(0.0)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));

        CourseResponse response = courseService.findById("c1");

        assertThat(response.getLectures()).isNotNull().isEmpty();
    }

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        Date created = new Date();
        Date updated = new Date();
        Course course = Course.builder()
                .id("c1")
                .title("My Course")
                .description("Learn things")
                .teacherId("t1")
                .status("published")
                .tags(List.of("tag1"))
                .thumbnailUrl("thumb.jpg")
                .language("ro")
                .enrollmentCount(10)
                .avgRating(4.5)
                .lectures(new ArrayList<>())
                .hidden(false)
                .quizId("q1")
                .createdAt(created)
                .updatedAt(updated)
                .build();

        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));

        CourseResponse response = courseService.findById("c1");

        assertThat(response.getId()).isEqualTo("c1");
        assertThat(response.getTitle()).isEqualTo("My Course");
        assertThat(response.getDescription()).isEqualTo("Learn things");
        assertThat(response.getTeacherId()).isEqualTo("t1");
        assertThat(response.getStatus()).isEqualTo("published");
        assertThat(response.getTags()).containsExactly("tag1");
        assertThat(response.getThumbnailUrl()).isEqualTo("thumb.jpg");
        assertThat(response.getLanguage()).isEqualTo("ro");
        assertThat(response.getEnrollmentCount()).isEqualTo(10);
        assertThat(response.getAvgRating()).isEqualTo(4.5);
        assertThat(response.isHidden()).isFalse();
        assertThat(response.getQuizId()).isEqualTo("q1");
        assertThat(response.getCreatedAt()).isEqualTo(created);
        assertThat(response.getUpdatedAt()).isEqualTo(updated);
    }
}
