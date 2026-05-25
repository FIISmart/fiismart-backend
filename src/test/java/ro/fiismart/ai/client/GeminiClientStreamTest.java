package ro.fiismart.ai.client;

import org.junit.jupiter.api.Test;
import ro.fiismart.ai.config.GeminiProperties;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for the streaming entry points on {@link GeminiClient}.
 *
 * <p>These exercise the SSE parsing pipeline ({@code consumeSse}) directly
 * — no real Gemini call needed — and the cancellation contract on
 * {@link GeminiClient.StreamCancelHandle}.
 *
 * <p>Constructor uses the all-args form (properties + restClient) because
 * Lombok's {@code @RequiredArgsConstructor} generates one; we don't need
 * a real RestClient since the streaming path uses java.net.http.HttpClient
 * directly.
 */
class GeminiClientStreamTest {

    private GeminiClient newClient() {
        GeminiProperties props = new GeminiProperties();
        props.setBaseUrl("http://localhost");
        props.setModel("test-model");
        props.setKey("test-key");
        props.setTimeoutSeconds(30);
        // RestClient is null-safe here: streamGenerate uses java.net.http,
        // and consumeSse / handleSseData never touch the RestClient field.
        return new GeminiClient(props, null);
    }

    @Test
    void consumeSse_forwardsEachTextDeltaAndFinalDone() throws Exception {
        GeminiClient client = newClient();

        // Three text chunks then a finishReason=STOP chunk, separated by
        // SSE event terminators (blank lines).
        String sse =
                "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello\"}]}}]}\n" +
                "\n" +
                "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\" world\"}]}}]}\n" +
                "\n" +
                "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"!\"}]}}]}\n" +
                "\n" +
                "data: {\"candidates\":[{\"finishReason\":\"STOP\"}]}\n" +
                "\n";

        List<GeminiStreamChunk> received = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(sse));

        client.consumeSse(reader, received::add, null);

        // 3 text deltas, then a terminal Done(STOP).
        assertThat(received).hasSize(4);
        assertThat(received.get(0)).isInstanceOf(GeminiStreamChunk.TextDelta.class);
        assertThat(((GeminiStreamChunk.TextDelta) received.get(0)).text()).isEqualTo("Hello");
        assertThat(((GeminiStreamChunk.TextDelta) received.get(1)).text()).isEqualTo(" world");
        assertThat(((GeminiStreamChunk.TextDelta) received.get(2)).text()).isEqualTo("!");

        assertThat(received.get(3)).isInstanceOf(GeminiStreamChunk.Done.class);
        assertThat(((GeminiStreamChunk.Done) received.get(3)).finishReason()).isEqualTo("STOP");
    }

    @Test
    void consumeSse_handlesMultiLineDataAndComments() throws Exception {
        GeminiClient client = newClient();

        // SSE allows comments starting with ":" and ignores them.
        // Gemini occasionally emits keep-alive comments before real events.
        String sse =
                ": keep-alive\n" +
                "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"chunk1\"}]}}]}\n" +
                "\n" +
                ": another comment\n" +
                "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"chunk2\"}]," +
                "\"role\":\"model\"},\"finishReason\":\"STOP\"}]}\n" +
                "\n";

        List<GeminiStreamChunk> received = new ArrayList<>();
        client.consumeSse(new BufferedReader(new StringReader(sse)), received::add, null);

        // Two text deltas + one terminal Done.
        assertThat(received).extracting(Object::getClass).containsExactly(
                GeminiStreamChunk.TextDelta.class,
                GeminiStreamChunk.TextDelta.class,
                GeminiStreamChunk.Done.class);
        assertThat(((GeminiStreamChunk.TextDelta) received.get(0)).text()).isEqualTo("chunk1");
        assertThat(((GeminiStreamChunk.TextDelta) received.get(1)).text()).isEqualTo("chunk2");
        assertThat(((GeminiStreamChunk.Done) received.get(2)).finishReason()).isEqualTo("STOP");
    }

    @Test
    void consumeSse_extractsFunctionCallChunk() throws Exception {
        GeminiClient client = newClient();

        String sse =
                "data: {\"candidates\":[{\"content\":{\"parts\":[" +
                "{\"functionCall\":{\"name\":\"createQuizDraft\"," +
                "\"args\":{\"topic\":\"Fotosinteza\",\"questionCount\":5}}}" +
                "]},\"finishReason\":\"STOP\"}]}\n" +
                "\n";

        List<GeminiStreamChunk> received = new ArrayList<>();
        client.consumeSse(new BufferedReader(new StringReader(sse)), received::add, null);

        assertThat(received).hasSize(2); // FunctionCall + Done
        assertThat(received.get(0)).isInstanceOf(GeminiStreamChunk.FunctionCall.class);
        GeminiStreamChunk.FunctionCall fc = (GeminiStreamChunk.FunctionCall) received.get(0);
        assertThat(fc.name()).isEqualTo("createQuizDraft");
        assertThat(fc.args())
                .containsEntry("topic", "Fotosinteza")
                .containsEntry("questionCount", 5);
    }

    @Test
    void consumeSse_stopsEmittingAfterCancellation() throws Exception {
        GeminiClient client = newClient();

        // A reader that yields one chunk per readLine call but delegates
        // to an underlying queue we can drain at the test's pace. We
        // flip the cancel handle after the first delta is received.
        String sse =
                "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"first\"}]}}]}\n" +
                "\n" +
                "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"second\"}]}}]}\n" +
                "\n" +
                "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"third\"}]}}]}\n" +
                "\n" +
                "data: {\"candidates\":[{\"finishReason\":\"STOP\"}]}\n" +
                "\n";

        GeminiClient.StreamCancelHandle cancel = new GeminiClient.StreamCancelHandle();
        List<GeminiStreamChunk> received = new ArrayList<>();
        AtomicInteger emitted = new AtomicInteger(0);

        client.consumeSse(new BufferedReader(new StringReader(sse)), chunk -> {
            received.add(chunk);
            if (chunk instanceof GeminiStreamChunk.TextDelta && emitted.incrementAndGet() == 1) {
                // Flip cancellation after the FIRST text delta — the loop
                // checks the flag at the next line boundary.
                cancel.cancel();
            }
        }, cancel);

        // We expect the first delta to arrive, then cancellation interrupts
        // before any further chunk or terminal Done is delivered.
        assertThat(received).hasSize(1);
        assertThat(received.get(0)).isInstanceOf(GeminiStreamChunk.TextDelta.class);
        assertThat(((GeminiStreamChunk.TextDelta) received.get(0)).text()).isEqualTo("first");
    }

    @Test
    void streamCancelHandle_defaultsToNotCancelled() {
        GeminiClient.StreamCancelHandle h = new GeminiClient.StreamCancelHandle();
        assertThat(h.isCancelled()).isFalse();
        h.cancel();
        assertThat(h.isCancelled()).isTrue();
    }

    /**
     * Cross-thread observability check: a streaming worker thread polling
     * {@link GeminiClient.StreamCancelHandle#isCancelled()} sees the flag
     * flip set by another thread. The field is {@code volatile} so this
     * is guaranteed; this test pins the contract so we'd catch a
     * regression if someone removed it.
     */
    @Test
    void streamCancelHandle_cancellationIsVisibleAcrossThreads() throws Exception {
        GeminiClient.StreamCancelHandle handle = new GeminiClient.StreamCancelHandle();
        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Boolean> observed = new AtomicReference<>(null);

        Thread observer = new Thread(() -> {
            started.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (System.nanoTime() < deadline) {
                if (handle.isCancelled()) {
                    observed.set(Boolean.TRUE);
                    return;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            observed.set(Boolean.FALSE);
        }, "stream-cancel-observer");

        observer.start();
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        handle.cancel();
        observer.join(TimeUnit.SECONDS.toMillis(3));

        assertThat(observed.get()).isEqualTo(Boolean.TRUE);
    }

    @Test
    void consumeSse_isQuietWhenStreamIsEmpty() {
        GeminiClient client = newClient();
        List<GeminiStreamChunk> received = new ArrayList<>();
        assertDoesNotThrow(() ->
                client.consumeSse(new BufferedReader(new StringReader("")), received::add, null));

        // Even on empty upstream we deliver a terminal Done so the caller's
        // accumulator + parse step has a known completion signal.
        assertThat(received).hasSize(1);
        assertThat(received.get(0)).isInstanceOf(GeminiStreamChunk.Done.class);
        assertThat(((GeminiStreamChunk.Done) received.get(0)).finishReason()).isEqualTo("STOP");
    }
}
