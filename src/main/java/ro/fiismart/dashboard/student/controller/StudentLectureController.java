package ro.fiismart.dashboard.student.controller;

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
    public List<StudentModuleDTO> getModules(@AuthenticationPrincipal String authenticatedStudentId,
                                              @PathVariable String studentId,
                                              @PathVariable String courseId) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Access denied to other student's courses");
        }
        return studentLectureService.getModules(studentId, courseId);
    }

    @GetMapping("/lectures/{lectureId}")
    public StudentLectureDetailDTO getLectureDetail(@AuthenticationPrincipal String authenticatedStudentId,
                                                     @PathVariable String studentId,
                                                     @PathVariable String courseId,
                                                     @PathVariable String lectureId) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Access denied to other student's courses");
        }
        return studentLectureService.getLectureDetail(studentId, courseId, lectureId);
    }

    @PutMapping("/lectures/{lectureId}/progress")
    public StudentLectureProgressResponse updateProgress(@AuthenticationPrincipal String authenticatedStudentId,
                                                          @PathVariable String studentId,
                                                          @PathVariable String courseId,
                                                          @PathVariable String lectureId,
                                                          @RequestBody StudentLectureProgressRequest req) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Access denied to other student's courses");
        }
        return studentLectureService.updateLectureProgress(studentId, courseId, lectureId, req);
    }
}
