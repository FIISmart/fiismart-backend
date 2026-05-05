package ro.fiismart.dashboard.student.controller;

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
                                              @PathVariable String courseId) {
        return studentLectureService.getModules(studentId, courseId);
    }

    @GetMapping("/lectures/{lectureId}")
    public StudentLectureDetailDTO getLectureDetail(@PathVariable String studentId,
                                                     @PathVariable String courseId,
                                                     @PathVariable String lectureId) {
        return studentLectureService.getLectureDetail(studentId, courseId, lectureId);
    }

    @PutMapping("/lectures/{lectureId}/progress")
    public StudentLectureProgressResponse updateProgress(@PathVariable String studentId,
                                                          @PathVariable String courseId,
                                                          @PathVariable String lectureId,
                                                          @RequestBody StudentLectureProgressRequest req) {
        return studentLectureService.updateLectureProgress(studentId, courseId, lectureId, req);
    }
}
