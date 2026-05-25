package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.Enrollment;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.dashboard.student.dto.StudentCourseHeaderDTO;

@Service
public class StudentCourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentQuizService studentQuizService;

    public StudentCourseService(CourseRepository courseRepository,
                                UserRepository userRepository,
                                EnrollmentRepository enrollmentRepository,
                                StudentQuizService studentQuizService) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentQuizService = studentQuizService;
    }

    public StudentCourseHeaderDTO getHeader(String studentId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        User teacher = course.getTeacherId() != null
                ? userRepository.findById(course.getTeacherId()).orElse(null)
                : null;

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElse(null);

        StudentCourseHeaderDTO dto = new StudentCourseHeaderDTO();
        dto.setCourseId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setThumbnailUrl(course.getThumbnailUrl());
        dto.setLanguage(course.getLanguage());
        dto.setStatus(course.getStatus());
        dto.setTags(course.getTags());
        dto.setTeacherId(course.getTeacherId());
        dto.setTeacherDisplayName(teacher != null ? teacher.getDisplayName() : "");
        dto.setAvgRating(course.getAvgRating());
        dto.setEnrollmentCount(course.getEnrollmentCount());
        dto.setEnrolled(enrollment != null);
        dto.setOverallProgress(enrollment != null ? enrollment.getOverallProgress() : 0);
        dto.setFinalQuiz(studentQuizService.getQuizStatus(studentId, courseId));
        return dto;
    }
}
