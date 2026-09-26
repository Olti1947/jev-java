package io.github.Olti1947.jev;

import io.github.Olti1947.jev.exception.JevValidationException;
import io.github.Olti1947.jev.model.Choice;
import io.github.Olti1947.jev.model.JevRequest;
import io.github.Olti1947.jev.model.Noul;
import org.junit.jupiter.api.Test;

import java.util.List;
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

        assertThrows(IllegalStateException.class, () -> client.evaluate(new Object(), List.of()));
        CompletionException error = assertThrows(CompletionException.class,
                () -> client.evaluateAsync(new Object(), List.of()).join());
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
