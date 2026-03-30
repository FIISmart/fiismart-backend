package com.fiismart.controller;

import com.fiismart.dto.InitialsDTO;
import com.fiismart.service.InitialsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class InitialsController {

    private final InitialsService initialsService;

    public InitialsController(InitialsService initialsService) {
        this.initialsService = initialsService;
    }

    @GetMapping("/{studentId}/initials")
    public ResponseEntity<InitialsDTO> getInitials(@PathVariable String studentId) {
        InitialsDTO dto = initialsService.getInitials(studentId);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }
}