package io.github.Olti1947.jev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.Olti1947.jev.model.ChoiceAnswer;
import io.github.Olti1947.jev.model.JevAnswer;
import io.github.Olti1947.jev.model.JevPrimitive;
import io.github.Olti1947.jev.model.JevResponse;
import io.github.Olti1947.jev.model.Noul;
import io.github.Olti1947.jev.model.NoulAnswer;
import io.github.Olti1947.jev.model.Score;
import io.github.Olti1947.jev.model.ScoreAnswer;
import io.github.Olti1947.jev.model.ScoreLevel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates between Jev's native wire format and the Vercel AI Gateway
 * evaluation-model protocol (spec version 4).
 *
 * <p>The two formats are nearly identical: both send {@code {state, questions}}
 * with questions keyed by name. The differences are that the gateway calls the
 * noul type {@code boolean} (answering with {@code probability} instead of
 * {@code noul}), takes the model id as a header rather than a body field, and
 * reports usage in camelCase.
 */
final class GatewayProtocol {

    static final String DEFAULT_BASE_URL = "https://ai-gateway.vercel.sh/v4/ai";
    static final String PROTOCOL_VERSION = "0.0.1";
    static final String SPEC_VERSION = "4";

    private GatewayProtocol() {
    }

    /**
     * Builds the gateway request body: {@code {"state": ..., "questions": {name: question}}}.
     */
    static ObjectNode buildBody(ObjectMapper mapper, Object state, List<JevPrimitive> primitives) {
        ObjectNode body = mapper.createObjectNode();
        body.set("state", mapper.valueToTree(state));

        ObjectNode questions = body.putObject("questions");
        for (JevPrimitive primitive : primitives) {
            ObjectNode question = mapper.valueToTree(primitive);
            if (primitive instanceof Noul) {
                question.put("type", "boolean");
            }
            questions.set(primitive.name(), question);
        }
        return body;
    }

    /**
     * Maps the gateway response onto the native {@link JevResponse} shape.
     */
    static JevResponse parseResponse(JsonNode root, List<JevPrimitive> primitives, String model) {
        JsonNode answers = root.path("answers");
        JsonNode confidences = root.path("providerMetadata").path("typesafe").path("confidence");

        Map<String, JevAnswer> mapped = new LinkedHashMap<>();
        for (JevPrimitive primitive : primitives) {
            JsonNode answer = answers.path(primitive.name());
            if (answer.isMissingNode()) {
                continue;
            }
            double confidence = confidences.path(primitive.name()).asDouble(Double.NaN);
            mapped.put(primitive.name(), toAnswer(primitive, answer, confidence));
        }

        JsonNode usage = root.path("usage");
        return new JevResponse(model, mapped, new JevResponse.Usage(
                usage.path("inputTokens").asLong(),
                usage.path("outputTokens").asLong()));
    }

    private static JevAnswer toAnswer(JevPrimitive primitive, JsonNode answer, double confidence) {
        return switch (answer.path("type").asText()) {
            case "boolean" -> new NoulAnswer(answer.path("probability").asDouble());
            case "choice" -> {
                Map<String, Double> probabilities = readProbabilities(answer.path("probabilities"));
                String choice = answer.path("choice").asText();
                yield new ChoiceAnswer(choice,
                        fallbackConfidence(confidence, probabilities == null ? null : probabilities.get(choice)),
                        probabilities);
            }
            case "score" -> {
                Map<String, Double> probabilities = readProbabilities(answer.path("probabilities"));
                Double top = probabilities == null ? null
                        : probabilities.values().stream().max(Double::compareTo).orElse(null);
                yield new ScoreAnswer(answer.path("score").asDouble(),
                        fallbackConfidence(confidence, top),
                        probabilities,
                        legendOf(primitive));
            }
            default -> throw new IllegalStateException(
                    "Unexpected gateway answer type: " + answer.path("type").asText());
        };
    }

    /**
     * The gateway reports confidence in provider metadata rather than on the
     * answer; when absent, fall back to the top option's probability.
     */
    private static double fallbackConfidence(double metadataConfidence, Double topProbability) {
        if (!Double.isNaN(metadataConfidence)) {
            return metadataConfidence;
        }
        return topProbability == null ? 0.0 : topProbability;
    }

    /**
     * Rebuilds the native score legend (level number -> description) from the
     * request's criteria, which the gateway does not echo back.
     */
    private static Map<String, String> legendOf(JevPrimitive primitive) {
        if (!(primitive instanceof Score score)) {
            return null;
        }
        Map<String, String> legend = new LinkedHashMap<>();
        for (int i = 0; i < score.criteria().size(); i++) {
            legend.put(String.valueOf(i), ScoreLevel.describe(score.criteria().get(i)));
        }
        return legend;
    }

    private static Map<String, Double> readProbabilities(JsonNode node) {
        if (!node.isObject()) {
            return null;
        }
        Map<String, Double> probabilities = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry ->
                probabilities.put(entry.getKey(), entry.getValue().asDouble()));
        return probabilities;
    }
}
