package io.github.Olti1947.jev;

import io.github.Olti1947.jev.exception.JevValidationException;
import io.github.Olti1947.jev.model.Choice;
import io.github.Olti1947.jev.model.JevPrimitive;
import io.github.Olti1947.jev.model.JevRequest;
import io.github.Olti1947.jev.model.JevResponse;
import io.github.Olti1947.jev.model.Noul;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class JevClientTest {

    @Test
    void testChoiceValidationThrowsOnEmptyOptions() {
        assertThrows(JevValidationException.class, () ->
                new Choice("intent", "Classify query", List.of())
        );
    }

    @Test
    void testClientBuilderRequiresApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
                JevClient.builder().build()
        );
    }

    @Test
    void closePreventsFurtherSyncAndAsyncRequests() {
        JevClient client = JevClient.builder().apiKey("test-key").build();
        client.close();

        List<JevPrimitive> primitives = List.of(new Noul("is_urgent", "Is this urgent?"));
        assertThrows(IllegalStateException.class, () -> client.evaluate(new Object(), primitives));
        CompletionException error = assertThrows(CompletionException.class,
                () -> client.evaluateAsync(new Object(), primitives).join());
        assertInstanceOf(IllegalStateException.class, error.getCause());
    }

    @Test
    void closePreventsFurtherRequestBasedSyncAndAsyncRequests() {
        JevClient client = JevClient.builder().apiKey("test-key").build();
        client.close();

        JevRequest request = JevRequest.builder()
                .state(new Object())
                .addPrimitive(new Noul("is_urgent", "Is this urgent?"))
                .build();

        assertThrows(IllegalStateException.class, () -> client.evaluate(request));
        CompletionException error = assertThrows(CompletionException.class,
                () -> client.evaluateAsync(request).join());
        assertInstanceOf(IllegalStateException.class, error.getCause());
    }

    @Test
    void closeIsIdempotentAndSupportsTryWithResources() {
        JevClient client = JevClient.builder().apiKey("test-key").build();

        assertDoesNotThrow(() -> {
            client.close();
            client.close();
        });

        assertDoesNotThrow(() -> {
            try (JevClient ignored = JevClient.builder().apiKey("test-key").build()) {
                // Closing an unused client is safe.
            }
        });
    }

    @Test
    void evaluateRejectsInvalidInputBeforeSending() {
        // Unreachable port: if validation were skipped, the call would fail with a
        // connection error (JevSerializationException) instead.
        JevClient client = JevClient.builder().apiKey("test-key").baseUrl("http://localhost:1").build();
        List<JevPrimitive> questions = List.of(new Noul("is_urgent", "Is this urgent?"));

        assertThrows(JevValidationException.class, () -> client.evaluate(null, questions));
        assertThrows(JevValidationException.class, () -> client.evaluate("state", List.of()));
        assertThrows(JevValidationException.class, () -> client.evaluate("state", List.of(
                new Noul("a", "?"), new Choice("a", "?", List.of("x", "y")))));
        assertThrows(JevValidationException.class, () -> client.evaluate((JevRequest) null));
    }

    @Test
    void evaluateAsyncReportsInvalidInputThroughTheFuture() {
        JevClient client = JevClient.builder().apiKey("test-key").baseUrl("http://localhost:1").build();
        List<JevPrimitive> questions = List.of(new Noul("is_urgent", "Is this urgent?"));

        List<CompletableFuture<JevResponse>> futures = List.of(
                client.evaluateAsync(null, questions),
                client.evaluateAsync("state", List.of()),
                client.evaluateAsync("state", List.of(new Noul("a", "?"), new Noul("a", "?"))),
                client.evaluateAsync((JevRequest) null));

        for (CompletableFuture<JevResponse> future : futures) {
            CompletionException error = assertThrows(CompletionException.class, future::join);
            assertInstanceOf(JevValidationException.class, error.getCause());
        }
    }

    @Test
    void evaluateChoiceFailsAfterClose() {
        JevClient client = JevClient.builder().apiKey("test-key").build();
        client.close();

        assertThrows(IllegalStateException.class,
                () -> client.evaluateChoice(new Object(), "Choose an option", TestChoice.class));
    }

    private enum TestChoice {
        FIRST,
        SECOND
    }
}
