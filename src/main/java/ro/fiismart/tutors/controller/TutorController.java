package ro.fiismart.tutors.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.fiismart.tutors.dto.TutorResponse;
import ro.fiismart.tutors.service.TutorService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tutors")
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping
    public List<TutorResponse> listTutors() {
        return tutorService.listTutors();
    }
}
