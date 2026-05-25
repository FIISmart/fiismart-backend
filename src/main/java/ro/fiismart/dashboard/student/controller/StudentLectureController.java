package ro.fiismart.dashboard.student.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.util.AuthUtils;
import ro.fiismart.dashboard.student.dto.*;
import ro.fiismart.dashboard.student.service.StudentLectureService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/{studentId}/courses/{courseId}")
@PreAuthorize("hasRole('STUDENT')")
public class StudentLectureController {

    private final StudentLectureService studentLectureService;

    public StudentLectureController(StudentLectureService studentLectureService) {
        this.studentLectureService = studentLectureService;
    }

    @GetMapping("/modules")
    public List<StudentModuleDTO> getModules(@PathVariable String studentId,
                                              @PathVariable String courseId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own data");
        }
        return studentLectureService.getModules(studentId, courseId);
    }

    @GetMapping("/lectures/{lectureId}")
    public StudentLectureDetailDTO getLectureDetail(@PathVariable String studentId,
                                                     @PathVariable String courseId,
                                                     @PathVariable String lectureId) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only access your own data");
        }
        return studentLectureService.getLectureDetail(studentId, courseId, lectureId);
    }

    @PutMapping("/lectures/{lectureId}/progress")
public StudentLectureProgressResponse updateProgress(@PathVariable String studentId,
                                                          @PathVariable String courseId,
                                                          @PathVariable String lectureId,
                                                          @Valid @RequestBody StudentLectureProgressRequest req) {
        if (!studentId.equals(AuthUtils.getCurrentUserId())) {
            throw new ForbiddenException("You can only update your own data");
        }
        return studentLectureService.updateLectureProgress(studentId, courseId, lectureId, req);
    }
}