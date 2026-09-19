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

/**
 * Entry-point client for interacting with the TypeSafe Jev System One API.
 */
public class JevClient {
    private final String apiKey;
    private final String baseUrl;
    private final boolean vercelGateway;
    private final String gatewayModel;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private JevClient(Builder builder) {
        this.apiKey = builder.apiKey;
        this.vercelGateway = builder.vercelGateway;
        this.gatewayModel = builder.gatewayModel;
        this.baseUrl = builder.baseUrl != null
                ? builder.baseUrl
                : (vercelGateway ? GatewayProtocol.DEFAULT_BASE_URL : "https://api.typesafe.ai");
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
        try {
            HttpRequest httpRequest = buildHttpRequest(state, primitives);
            long start = System.nanoTime();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new JevApiException(response.statusCode(), response.body());
            }

            return parseResponse(response.body(), primitives, (System.nanoTime() - start) / 1_000_000);
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
        try {
            HttpRequest httpRequest = buildHttpRequest(state, primitives);
            long start = System.nanoTime();

            return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new JevApiException(response.statusCode(), response.body());
                        }
                        try {
                            return parseResponse(response.body(), primitives, (System.nanoTime() - start) / 1_000_000);
                        } catch (Exception e) {
                            throw new JevSerializationException("Failed to deserialize response", e);
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new JevSerializationException("Failed to serialize request", e));
        }
    }

    private HttpRequest buildHttpRequest(Object state, List<JevPrimitive> primitives) throws Exception {
        String jsonBody;
        HttpRequest.Builder request;

        if (vercelGateway) {
            jsonBody = objectMapper.writeValueAsString(GatewayProtocol.buildBody(objectMapper, state, primitives));
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/evaluation-model"))
                    .header("ai-gateway-protocol-version", GatewayProtocol.PROTOCOL_VERSION)
                    .header("ai-evaluation-model-specification-version", GatewayProtocol.SPEC_VERSION)
                    .header("ai-model-id", gatewayModel);
        } else {
            jsonBody = objectMapper.writeValueAsString(new JevRequest(state, primitives));
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/systemone"));
        }

        return request
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("User-Agent", "jev-java-sdk/1.0.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }

    private JevResponse parseResponse(String body, List<JevPrimitive> primitives, long latencyMs) throws Exception {
        if (vercelGateway) {
            return GatewayProtocol.parseResponse(objectMapper.readTree(body), primitives, latencyMs);
        }
        return objectMapper.readValue(body, JevResponse.class);
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
        private String baseUrl;
        private boolean vercelGateway;
        private String gatewayModel = "typesafe-ai/jev";
        private Duration timeout = Duration.ofSeconds(10);

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Routes requests through the Vercel AI Gateway instead of the native
         * TypeSafe API. Use an AI Gateway API key (vck_...) as {@code apiKey}.
         */
        public Builder vercelGateway() {
            this.vercelGateway = true;
            return this;
        }

        /**
         * Gateway model id to evaluate against (default "typesafe-ai/jev").
         * Only used together with {@link #vercelGateway()}.
         */
        public Builder gatewayModel(String gatewayModel) {
            this.gatewayModel = gatewayModel;
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