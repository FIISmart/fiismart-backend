package ro.fiismart.ai.dto.response;

public record PdfAiGenerateResponse(
        String summary,
        AiQuizDraftDTO quiz
) {}
