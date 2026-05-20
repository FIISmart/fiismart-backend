package ro.fiismart.dashboard.student.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.dashboard.student.dto.StudentCourseHeaderDTO;
import ro.fiismart.dashboard.student.dto.StudentQuizStatusDTO;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentQuizService studentQuizService;

    @InjectMocks
    private StudentCourseService studentCourseService;

    @Test
    void getHeader_courseNotFound_throws() {
        when(courseRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentCourseService.getHeader("s1", "noId"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void getHeader_withEnrollment_setsEnrolledAndProgress() {
        Course course = Course.builder().id("c1").title("Matematică")
                .teacherId("t1").tags(new ArrayList<>()).build();
        User teacher = User.builder().id("t1").displayName("Prof. Ion").build();
        Enrollment enrollment = Enrollment.builder()
                .studentId("s1").courseId("c1").overallProgress(55).build();
        StudentQuizStatusDTO quizStatus = new StudentQuizStatusDTO();

        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));
        when(userRepository.findById("t1")).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.findByStudentIdAndCourseId("s1", "c1"))
                .thenReturn(Optional.of(enrollment));
        when(studentQuizService.getQuizStatus("s1", "c1")).thenReturn(quizStatus);

        StudentCourseHeaderDTO result = studentCourseService.getHeader("s1", "c1");

        assertThat(result.getCourseId()).isEqualTo("c1");
        assertThat(result.getTitle()).isEqualTo("Matematică");
        assertThat(result.isEnrolled()).isTrue();
        assertThat(result.getOverallProgress()).isEqualTo(55);
        assertThat(result.getTeacherDisplayName()).isEqualTo("Prof. Ion");
    }

    @Test
    void getHeader_withoutEnrollment_notEnrolledZeroProgress() {
        Course course = Course.builder().id("c1").title("Fizică")
                .teacherId(null).tags(new ArrayList<>()).build();
        StudentQuizStatusDTO quizStatus = new StudentQuizStatusDTO();

        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId("s1", "c1"))
                .thenReturn(Optional.empty());
        when(studentQuizService.getQuizStatus("s1", "c1")).thenReturn(quizStatus);

        StudentCourseHeaderDTO result = studentCourseService.getHeader("s1", "c1");

        assertThat(result.isEnrolled()).isFalse();
        assertThat(result.getOverallProgress()).isEqualTo(0);
        assertThat(result.getTeacherDisplayName()).isEqualTo("");
    }

    @Test
    void getHeader_teacherNotFound_usesEmptyName() {
        Course course = Course.builder().id("c1").title("Chimie")
                .teacherId("noTeacher").tags(new ArrayList<>()).build();
        StudentQuizStatusDTO quizStatus = new StudentQuizStatusDTO();

        when(courseRepository.findById("c1")).thenReturn(Optional.of(course));
        when(userRepository.findById("noTeacher")).thenReturn(Optional.empty());
        when(enrollmentRepository.findByStudentIdAndCourseId("s1", "c1"))
                .thenReturn(Optional.empty());
        when(studentQuizService.getQuizStatus("s1", "c1")).thenReturn(quizStatus);

        StudentCourseHeaderDTO result = studentCourseService.getHeader("s1", "c1");

        assertThat(result.getTeacherDisplayName()).isEqualTo("");
    }
}
