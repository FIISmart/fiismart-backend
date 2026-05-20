package ro.fiismart.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ro.fiismart.ai.client.GeminiClient;
import ro.fiismart.ai.client.GeminiException;
import ro.fiismart.ai.dto.response.AiQuizDraftDTO;
import ro.fiismart.ai.dto.response.AiQuizQuestionDTO;
import ro.fiismart.ai.dto.response.PdfAiGenerateResponse;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfAiService {

    private static final long MAX_PDF_BYTES = 15L * 1024L * 1024L;
    private static final int MIN_QUESTIONS = 3;
    private static final int MAX_QUESTIONS = 10;
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String DEFAULT_LANGUAGE = "ro";
    private static final List<String> SUPPORTED_LANGUAGES = List.of("ro", "en");
    private static final String MULTIPLE_CHOICE = "multiple_choice";

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public PdfAiGenerateResponse generate(MultipartFile pdf, int questionCount, String language)
            throws IOException {
        validatePdf(pdf);

        if (questionCount < MIN_QUESTIONS || questionCount > MAX_QUESTIONS) {
            throw new IllegalArgumentException(
                    "questionCount must be between " + MIN_QUESTIONS + " and " + MAX_QUESTIONS);
        }

        String normalizedLanguage = (language == null || language.isBlank())
                ? DEFAULT_LANGUAGE
                : language.toLowerCase();
        if (!SUPPORTED_LANGUAGES.contains(normalizedLanguage)) {
            throw new IllegalArgumentException("language must be one of: ro, en");
        }

        String prompt = PdfAiPrompts.buildPrompt(questionCount, normalizedLanguage);
        String json = geminiClient.generateJson(prompt, pdf.getBytes(), PdfAiPrompts.RESPONSE_SCHEMA);

        PdfAiGenerateResponse response;
        try {
            response = objectMapper.readValue(json, PdfAiGenerateResponse.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize Gemini JSON into PdfAiGenerateResponse: {}",
                    e.getClass().getSimpleName());
            throw new GeminiException("Gemini returned invalid quiz structure", e);
        }

        validateGeneratedResponse(response);
        return response;
    }

    private void validatePdf(MultipartFile pdf) {
        if (pdf == null || pdf.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required");
        }
        String contentType = pdf.getContentType();
        if (contentType == null || !PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("File must be application/pdf");
        }
        if (pdf.getSize() > MAX_PDF_BYTES) {
            throw new IllegalArgumentException("PDF exceeds 15 MB limit");
        }
    }

    private void validateGeneratedResponse(PdfAiGenerateResponse response) {
        if (response == null || response.quiz() == null) {
            throw new GeminiException("Gemini returned invalid quiz structure");
        }
        AiQuizDraftDTO quiz = response.quiz();
        List<AiQuizQuestionDTO> questions = quiz.questions();
        if (questions == null || questions.isEmpty()) {
            throw new GeminiException("Gemini returned invalid quiz structure");
        }
        for (AiQuizQuestionDTO q : questions) {
            if (q == null
                    || q.options() == null
                    || q.options().size() != 4
                    || q.correctIdx() == null
                    || q.correctIdx() < 0
                    || q.correctIdx() > 3
                    || !MULTIPLE_CHOICE.equals(q.type())) {
                throw new GeminiException("Gemini returned invalid quiz structure");
            }
        }
    }
}
