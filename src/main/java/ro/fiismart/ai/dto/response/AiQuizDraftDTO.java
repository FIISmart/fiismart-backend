package ro.fiismart.ai.dto.response;

import java.util.List;

public record AiQuizDraftDTO(
        String title,
        Integer passingScore,
        Integer timeLimit,
        List<AiQuizQuestionDTO> questions
) {}
