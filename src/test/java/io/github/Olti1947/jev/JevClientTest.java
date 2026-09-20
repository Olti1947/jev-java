package io.github.Olti1947.jev;

import io.github.Olti1947.jev.exception.JevValidationException;
import io.github.Olti1947.jev.model.Choice;
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
}
