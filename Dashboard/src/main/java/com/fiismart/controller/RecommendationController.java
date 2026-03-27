package com.fiismart.controller;

import com.fiismart.dto.RecommendationDTO;
import com.fiismart.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{studentId}/recommendations")
    public ResponseEntity<RecommendationDTO> getRecommendation(@PathVariable String studentId) {
        RecommendationDTO recommendation = recommendationService.getRecommendation(studentId);
        if (recommendation == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(recommendation);
    }
}