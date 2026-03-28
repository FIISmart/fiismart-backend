package com.fiismart.student.dashboard.controller;

import com.fiismart.student.dashboard.dto.StatsDTO;
import com.fiismart.student.dashboard.service.StatsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/{studentId}/stats")
    public StatsDTO getStats(@PathVariable String studentId) {
        return statsService.getStats(studentId);
    }
}
