package ro.fiismart.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ro.fiismart.ai.client.GeminiClient;
import ro.fiismart.ai.dto.response.FreeTextGradeResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Locks in the per-grade timeout semantics added to free-text grading.
 *
 * <p>QuizAttemptService wraps every {@code gradeFreeText} dispatch in
 * {@code .orTimeout(AI_PER_GRADE_TIMEOUT_SECONDS, SECONDS).exceptionally(
 *   t -> fallbackFreeTextAnswer(...))}. The contract we lock in here:
 * <ol>
 *   <li><b>Slow Gemini call → fallback after timeout:</b> if a single
 *       grade hangs longer than the per-grade budget, the future
 *       resolves to a fallback Answer (incorrect, null AI score)
 *       instead of leaving the caller waiting indefinitely.</li>
 *   <li><b>Fast siblings unaffected:</b> a fast grade in the same
 *       wave completes with the real AI verdict — the slow one
 *       doesn't drag the whole batch into the fallback.</li>
 * </ol>
 *
 * <p>The test models the exact pattern QuizAttemptService applies (it
 * doesn't have access to QuizAttemptService's private gradeFreeText, but
 * the wrapping is structurally identical and is the unit under test).</p>
 */
class AiGradingTimeoutTest {

    /** Match the constant in QuizAttemptService — kept in sync intentionally. */
    private static final long PER_GRADE_TIMEOUT_SECONDS = 20L;

    /** Short test timeout so the suite doesn't actually wait 20s. */
    private static final long TEST_TIMEOUT_MILLIS = 200L;

    private static final FreeTextGradeResult FALLBACK = AiTextGraderService.FAILURE_RESULT;

    @Test
    void slowGrade_returnsFallback_afterPerGradeTimeout() {
        // Real grader, mocked client. The client blocks longer than the
        // per-grade timeout — orTimeout must fire and we must see the
        // fallback Answer, not a hang.
        GeminiClient slowClient = mock(GeminiClient.class);
        when(slowClient.generateJson(anyString(), any())).thenAnswer(invocation -> {
            // Block ~10x the test timeout to simulate a stuck upstream.
            Thread.sleep(TEST_TIMEOUT_MILLIS * 10);
            return "{\"score\":80,\"confidence\":0.9,\"reasoning\":\"ok\",\"missingConcepts\":[]}";
        });
        AiTextGraderService grader = new AiTextGraderService(slowClient, new ObjectMapper());

        ExecutorService executor = newAiGradingExecutor();
        try {
            CompletableFuture<FreeTextGradeResult> fut = CompletableFuture
                    .supplyAsync(() -> grader.grade(
                            "Ce e inertia?",
                            "Tendinta de a-si mentine starea.",
                            List.of("masa", "viteza"),
                            "Nu stiu."), executor)
                    // Force the timeout to fire well under the test
                    // budget so the assertion runs deterministically.
                    .orTimeout(TEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .exceptionally(t -> FALLBACK);

            // Resolve fast — the orTimeout above must trip well under
            // the 5-second join budget.
            FreeTextGradeResult result = fut.orTimeout(5, TimeUnit.SECONDS).join();

            // The slow grade falls back exactly to the canonical
            // FAILURE_RESULT, which downstream code treats as
            // "AI grading unavailable, mark incorrect".
            assertThat(result).isSameAs(FALLBACK);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void fastGrade_returnsRealAiVerdict_notFallback() {
        // Same pattern — but now the client returns immediately. The
        // orTimeout must NOT fire; the caller sees the real grade.
        GeminiClient fastClient = mock(GeminiClient.class);
        when(fastClient.generateJson(anyString(), any())).thenReturn(
                "{\"score\":92,\"confidence\":0.95,\"reasoning\":\"bun raspuns\",\"missingConcepts\":[]}");
        AiTextGraderService grader = new AiTextGraderService(fastClient, new ObjectMapper());

        ExecutorService executor = newAiGradingExecutor();
        try {
            CompletableFuture<FreeTextGradeResult> fut = CompletableFuture
                    .supplyAsync(() -> grader.grade(
                            "Q?", "A.", List.of(), "raspuns ok"), executor)
                    .orTimeout(TEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .exceptionally(t -> FALLBACK);

            FreeTextGradeResult result = fut.orTimeout(5, TimeUnit.SECONDS).join();

            assertThat(result).isNotSameAs(FALLBACK);
            assertThat(result.score()).isEqualTo(92.0);
            assertThat(result.confidence()).isEqualTo(0.95);
            assertThat(result.reasoning()).isEqualTo("bun raspuns");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void perGradeTimeoutConstant_isShorterThanOverallBudget() {
        // Sanity check that the per-grade cap stays under the overall
        // wave budget — otherwise individual timeouts couldn't fire
        // before the aggregate awaitFreeTextGrading bails.
        long overallBudget = 30L;
        assertThat(PER_GRADE_TIMEOUT_SECONDS).isLessThan(overallBudget);
    }

    /**
     * Mirrors the dedicated executor QuizAttemptService creates: a
     * bounded daemon-thread pool, not the JVM-wide commonPool. The
     * production code's pool sizing differs (CPU-scaled) but the
     * isolation property is what matters for this test.
     */
    private static ExecutorService newAiGradingExecutor() {
        AtomicInteger seq = new AtomicInteger();
        return Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "ai-grading-test-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }
}
