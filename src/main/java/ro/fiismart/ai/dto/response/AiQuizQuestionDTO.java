package ro.fiismart.ai.dto.response;

import java.util.List;

public record AiQuizQuestionDTO(
        String text,
        String type,
        List<String> options,
        Integer correctIdx,
        String explanation
) {}
