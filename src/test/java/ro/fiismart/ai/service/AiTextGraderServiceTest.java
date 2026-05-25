package ro.fiismart.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ro.fiismart.ai.client.GeminiClient;
import ro.fiismart.ai.client.GeminiException;
import ro.fiismart.ai.dto.response.FreeTextGradeResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiTextGraderService}.
 *
 * <p>Two behaviours we lock in:
 * <ol>
 *   <li><b>Happy path:</b> Gemini returns a well-formed JSON grade →
 *       service parses and surfaces score / confidence / reasoning /
 *       missingConcepts exactly as received.</li>
 *   <li><b>Failure path:</b> Gemini throws (e.g. exhausted retries on a
 *       503) → service NEVER throws; it returns the canonical fallback
 *       so the calling quiz submission can keep grading the rest of the
 *       questions.</li>
 * </ol></p>
 *
 * <p>Also covers two important edge cases: empty student answers must
 * short-circuit without an AI round-trip, and a malformed Gemini
 * response degrades to the same safe fallback.</p>
 */
class AiTextGraderServiceTest {

    /**
     * Convenience factory. The grader takes a real ObjectMapper because we
     * actually need it to parse Gemini's JSON output; only the GeminiClient
     * is mocked.
     */
    private AiTextGraderService newGrader(GeminiClient mockClient) {
        return new AiTextGraderService(mockClient, new ObjectMapper());
    }

    @Test
    void grade_happyPath_parsesScoreConfidenceAndMissingConcepts() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.generateJson(anyString(), any()))
                .thenReturn("""
                        {
                          "score": 85.5,
                          "confidence": 0.92,
                          "reasoning": "Raspuns bun, acopera majoritatea conceptelor.",
                          "missingConcepts": ["conceptul X"]
                        }
                        """);

        FreeTextGradeResult result = newGrader(client).grade(
                "Ce este fotosinteza?",
                "Proces prin care plantele transforma lumina in energie chimica.",
                List.of("clorofila", "CO2", "lumina"),
                "Plantele folosesc lumina solara si CO2 pentru a face zahar.");

        assertThat(result).isNotNull();
        assertThat(result.score()).isEqualTo(85.5);
        assertThat(result.confidence()).isEqualTo(0.92);
        assertThat(result.reasoning()).isEqualTo("Raspuns bun, acopera majoritatea conceptelor.");
        assertThat(result.missingConcepts()).containsExactly("conceptul X");
    }

    @Test
    void grade_geminiThrows_returnsFallbackInsteadOfPropagating() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.generateJson(anyString(), any()))
                .thenThrow(new GeminiException("Gemini upstream busy after 3 attempts"));

        FreeTextGradeResult result = newGrader(client).grade(
                "Ce este oxidarea?",
                "Reactie chimica in care un atom pierde electroni.",
                List.of("electroni", "reactie"),
                "Cand ceva ruginește.");

        // The contract: NEVER throw, ALWAYS return a result, with the score
        // null so the caller marks the answer incorrect.
        assertThat(result).isNotNull();
        assertThat(result.score()).isNull();
        assertThat(result.confidence()).isEqualTo(0.0);
        assertThat(result.reasoning()).isEqualTo("Evaluare AI esuata.");
        assertThat(result.missingConcepts()).isEmpty();
    }

    @Test
    void grade_emptyStudentAnswer_shortCircuitsWithoutCallingGemini() {
        GeminiClient client = mock(GeminiClient.class);

        FreeTextGradeResult result = newGrader(client).grade(
                "Cum functioneaza memoria virtuala?",
                "MMU mapeaza pagini virtuale la pagini fizice.",
                List.of("paginare", "MMU"),
                "   ");

        // Empty / whitespace answers are graded as 0/100 with full
        // confidence — no need to spend an API call to learn that.
        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.confidence()).isEqualTo(1.0);
        assertThat(result.reasoning()).isEqualTo("Raspuns gol.");
        assertThat(result.missingConcepts()).containsExactlyInAnyOrder("paginare", "MMU");
        verify(client, never()).generateJson(anyString(), any());
    }

    @Test
    void grade_malformedResponse_returnsFallback() {
        GeminiClient client = mock(GeminiClient.class);
        // Missing required fields (no score, no confidence) — must degrade.
        when(client.generateJson(anyString(), any()))
                .thenReturn("{\"reasoning\":\"only this field\"}");

        FreeTextGradeResult result = newGrader(client).grade(
                "Ce e HTTP?",
                "Protocol pentru transferul de hipertext.",
                List.of("protocol"),
                "Un mod de a face requests.");

        assertThat(result.score()).isNull();
        assertThat(result.confidence()).isEqualTo(0.0);
        assertThat(result.reasoning()).isEqualTo("Evaluare AI esuata.");
        assertThat(result.missingConcepts()).isEmpty();
    }

    @Test
    void grade_scoreClampedToValidRange() {
        GeminiClient client = mock(GeminiClient.class);
        // Gemini occasionally ignores schema bounds — we defensively clamp.
        when(client.generateJson(anyString(), any()))
                .thenReturn("""
                        {
                          "score": 120,
                          "confidence": 1.5,
                          "reasoning": "ok",
                          "missingConcepts": []
                        }
                        """);

        FreeTextGradeResult result = newGrader(client).grade(
                "Q?", "A.", List.of(), "answer");

        assertThat(result.score()).isEqualTo(100.0);
        assertThat(result.confidence()).isEqualTo(1.0);
    }

    @Test
    void buildPrompt_includesAllRubricSections() {
        String prompt = AiTextGraderService.buildPrompt(
                "Definiti polimorfismul.",
                "Polimorfismul permite metodelor sa aiba implementari diferite.",
                List.of("override", "interfata"),
                "Mostenirea face polimorfism.");

        // Surface check on the Romanian rubric prompt so nothing in the
        // template silently regresses.
        assertThat(prompt).contains("Esti corector de examen");
        assertThat(prompt).contains("Intrebare: Definiti polimorfismul.");
        assertThat(prompt).contains("Raspuns model: Polimorfismul permite");
        assertThat(prompt).contains("Concepte cheie obligatorii: override, interfata");
        assertThat(prompt).contains("Raspuns student: Mostenirea face polimorfism.");
        assertThat(prompt).contains("\"score\": 0-100");
    }
}
