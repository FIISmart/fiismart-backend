package ro.fiismart.dashboard.student.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.dashboard.student.dto.*;
import ro.fiismart.dashboard.student.service.StudentLectureService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/{studentId}/courses/{courseId}")
public class StudentLectureController {

    private final StudentLectureService studentLectureService;

    public StudentLectureController(StudentLectureService studentLectureService) {
        this.studentLectureService = studentLectureService;
    }

    @GetMapping("/modules")
    public List<StudentModuleDTO> getModules(@PathVariable String studentId,
                                              @PathVariable String courseId,
                                              @AuthenticationPrincipal String authenticatedUserId) {
        assertOwnStudentPath(studentId, authenticatedUserId);
        return studentLectureService.getModules(studentId, courseId);
    }

    @GetMapping("/lectures/{lectureId}")
    public StudentLectureDetailDTO getLectureDetail(@PathVariable String studentId,
                                                     @PathVariable String courseId,
                                                     @PathVariable String lectureId,
                                                     @AuthenticationPrincipal String authenticatedUserId) {
        assertOwnStudentPath(studentId, authenticatedUserId);
        return studentLectureService.getLectureDetail(studentId, courseId, lectureId);
    }

    @PutMapping("/lectures/{lectureId}/progress")
    public StudentLectureProgressResponse updateProgress(@PathVariable String studentId,
                                                          @PathVariable String courseId,
                                                          @PathVariable String lectureId,
                                                          @AuthenticationPrincipal String authenticatedUserId,
                                                          @Valid @RequestBody StudentLectureProgressRequest req) {
        assertOwnStudentPath(studentId, authenticatedUserId);
        return studentLectureService.updateLectureProgress(studentId, courseId, lectureId, req);
    }

    private void assertOwnStudentPath(String studentId, String authenticatedUserId) {
        if (authenticatedUserId == null || !authenticatedUserId.equals(studentId)) {
            throw new AccessDeniedException("Cannot access another student's course data");
        }
    }
}
