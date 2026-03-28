package com.fiismart.student.dashboard.controller;

import com.fiismart.student.dashboard.dto.CourseSummaryDTO;
import com.fiismart.student.dashboard.service.CoursesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class CoursesController {

    private final CoursesService coursesService;

    public CoursesController(CoursesService coursesService) {
        this.coursesService = coursesService;
    }

    @GetMapping("/{studentId}/courses")
    public List<CourseSummaryDTO> getCourses(@PathVariable String studentId) {
        return coursesService.getCourses(studentId);
    }
}

