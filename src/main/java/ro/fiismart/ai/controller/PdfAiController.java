package ro.fiismart.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ro.fiismart.ai.dto.response.PdfAiGenerateResponse;
import ro.fiismart.ai.service.PdfAiService;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class PdfAiController {

    private final PdfAiService service;

    @PostMapping(value = "/pdf/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROFESSOR')")
    public PdfAiGenerateResponse generate(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "5") int questionCount,
            @RequestParam(defaultValue = "ro") String language,
            @AuthenticationPrincipal String userId) throws IOException {
        log.info("AI PDF generate: user={} file={} size={} qCount={} lang={}",
                userId, file.getOriginalFilename(), file.getSize(), questionCount, language);
        return service.generate(file, questionCount, language);
    }
}
