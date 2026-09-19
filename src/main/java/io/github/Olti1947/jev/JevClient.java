package io.github.Olti1947.jev;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Olti1947.jev.exception.JevApiException;
import io.github.Olti1947.jev.exception.JevSerializationException;
import io.github.Olti1947.jev.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Entry-point client for interacting with the TypeSafe Jev System One API.
 */
public class JevClient {
    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private JevClient(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(builder.timeout)
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Executes a synchronous evaluation over state using the specified primitives.
     */
    public JevResponse evaluate(Object state, List<JevPrimitive> primitives) {
        JevRequest request = new JevRequest(state, primitives);
        try {
            String jsonBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/systemone"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", "jev-java-sdk/1.0.0")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<InputStream> response =
        httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

if (response.statusCode() != 200) {
    String responseBody = new String(
            response.body().readAllBytes(),
            StandardCharsets.UTF_8
    );
    throw new JevApiException(response.statusCode(), responseBody);
}

return objectMapper.readValue(response.body(), JevResponse.class);
        } catch (JevApiException e) {
            throw e;
        } catch (Exception e) {
            throw new JevSerializationException("Failed to execute Jev request", e);
        }
    }

    /**
     * Executes an asynchronous evaluation returning a CompletableFuture.
     */
    public CompletableFuture<JevResponse> evaluateAsync(Object state, List<JevPrimitive> primitives) {
        JevRequest request = new JevRequest(state, primitives);
        try {
            String jsonBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/systemone"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", "jev-java-sdk/1.0.0")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

           return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
        .thenApply(response -> {
            try {
                if (response.statusCode() != 200) {
                    String responseBody = new String(
                            response.body().readAllBytes(),
                            StandardCharsets.UTF_8
                    );
                    throw new JevApiException(response.statusCode(), responseBody);
                }

                return objectMapper.readValue(response.body(), JevResponse.class);

            } catch (JevApiException e) {
                throw e;
            } catch (Exception e) {
                throw new JevSerializationException(
                        "Failed to deserialize response", e
                );
            }
        });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new JevSerializationException("Failed to serialize request", e));
        }
    }

    /**
     * Helper method to map a Choice primitive directly to a Java Enum type.
     */
    public <E extends Enum<E>> E evaluateChoice(Object state, String instructions, Class<E> enumClass) {
        List<String> options = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .toList();

        Choice choice = new Choice("choice", instructions, options);
        JevResponse response = evaluate(state, List.of(choice));

        String value = response.results().get(0).value();
        return Enum.valueOf(enumClass, value);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static JevClient create(String apiKey) {
        return builder().apiKey(apiKey).build();
    }

    public static class Builder {
        private String apiKey;
        private String baseUrl = "https://api.typesafe.ai";
        private Duration timeout = Duration.ofSeconds(10);

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public JevClient build() {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("API Key must be provided.");
            }
            return new JevClient(this);
        }
    }
}