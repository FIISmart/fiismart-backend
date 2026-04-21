package com.fiismart.backend.controller.student;

import com.fiismart.backend.dto.student.StudentLectureDetailDTO;
import com.fiismart.backend.dto.student.StudentLectureProgressRequest;
import com.fiismart.backend.dto.student.StudentLectureProgressResponse;
import com.fiismart.backend.dto.student.StudentModuleDTO;
import com.fiismart.backend.service.student.StudentLectureService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students/{studentId}/courses/{courseId}")
@CrossOrigin(origins = "*")
public class StudentLectureController {

    private final StudentLectureService studentLectureService;

    public StudentLectureController(StudentLectureService studentLectureService) {
        this.studentLectureService = studentLectureService;
    }

    @GetMapping("/modules")
    public List<StudentModuleDTO> getModules(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        return studentLectureService.getModules(studentId, courseId);
    }

    @GetMapping("/lectures/{lectureId}")
    public StudentLectureDetailDTO getLectureDetail(
            @PathVariable String studentId,
            @PathVariable String courseId,
            @PathVariable String lectureId) {
        return studentLectureService.getLectureDetail(studentId, courseId, lectureId);
    }

    @PutMapping("/lectures/{lectureId}/progress")
    public StudentLectureProgressResponse updateLectureProgress(
            @PathVariable String studentId,
            @PathVariable String courseId,
            @PathVariable String lectureId,
            @RequestBody StudentLectureProgressRequest request) {
        return studentLectureService.updateLectureProgress(studentId, courseId, lectureId, request);
    }
}