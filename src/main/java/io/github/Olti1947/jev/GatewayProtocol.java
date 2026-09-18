package io.github.Olti1947.jev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.Olti1947.jev.exception.JevValidationException;
import io.github.Olti1947.jev.model.Choice;
import io.github.Olti1947.jev.model.JevPrimitive;
import io.github.Olti1947.jev.model.JevResponse;
import io.github.Olti1947.jev.model.Noul;
import io.github.Olti1947.jev.model.Score;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates between Jev's native primitives and the Vercel AI Gateway
 * evaluation-model wire protocol (spec version 4).
 */
final class GatewayProtocol {

    static final String DEFAULT_BASE_URL = "https://ai-gateway.vercel.sh/v4/ai";
    static final String PROTOCOL_VERSION = "0.0.1";
    static final String SPEC_VERSION = "4";

    private GatewayProtocol() {
    }

    /**
     * Builds the gateway request body: {"state": ..., "questions": {name: question}}.
     */
    static ObjectNode buildBody(ObjectMapper mapper, Object state, List<JevPrimitive> primitives) {
        ObjectNode body = mapper.createObjectNode();
        body.set("state", mapper.valueToTree(state));

        ObjectNode questions = body.putObject("questions");
        for (JevPrimitive primitive : primitives) {
            questions.set(primitive.name(), toQuestion(mapper, primitive));
        }
        return body;
    }

    private static ObjectNode toQuestion(ObjectMapper mapper, JevPrimitive primitive) {
        ObjectNode question = mapper.createObjectNode();
        question.put("instructions", primitive.instructions());

        if (primitive instanceof Choice choice) {
            question.put("type", "choice");
            ObjectNode criteria = question.putObject("criteria");
            if (choice.criteria() != null && !choice.criteria().isEmpty()) {
                choice.criteria().forEach(criteria::put);
            } else {
                // The gateway requires option descriptions; fall back to the option name itself.
                choice.options().forEach(option -> criteria.put(option, option));
            }
        } else if (primitive instanceof Noul noul) {
            question.put("type", "boolean");
            if (noul.criteria() != null && !noul.criteria().isEmpty()) {
                ObjectNode criteria = question.putObject("criteria");
                noul.criteria().forEach(criteria::put);
            }
        } else if (primitive instanceof Score score) {
            question.put("type", "score");
            if (score.legend() == null || score.legend().size() < 2) {
                throw new JevValidationException(
                        "Score over the gateway requires a legend with at least two ordered levels; "
                                + "min/max ranges are only supported by the native TypeSafe API.");
            }
            var criteria = question.putArray("criteria");
            score.legend().forEach((label, description) ->
                    criteria.add(description == null || description.isBlank()
                            ? label
                            : label + ": " + description));
        } else {
            throw new JevValidationException("Unsupported primitive type: " + primitive.getClass());
        }
        return question;
    }

    /**
     * Maps the gateway response back onto Jev's native {@link JevResponse} shape.
     * Result order follows the request's primitive order.
     */
    static JevResponse parseResponse(JsonNode root, List<JevPrimitive> primitives, long latencyMs) {
        JsonNode answers = root.path("answers");
        List<JevResponse.DecisionResult> results = new ArrayList<>();

        for (JevPrimitive primitive : primitives) {
            JsonNode answer = answers.path(primitive.name());
            if (answer.isMissingNode()) {
                continue;
            }
            results.add(toDecisionResult(primitive.name(), answer));
        }

        JsonNode idNode = root.path("providerMetadata").path("gateway").path("generationId");
        String id = idNode.isTextual() ? idNode.asText() : null;
        return new JevResponse(id, results, latencyMs);
    }

    private static JevResponse.DecisionResult toDecisionResult(String name, JsonNode answer) {
        String type = answer.path("type").asText();
        Map<String, Double> probabilities = readProbabilities(answer.path("probabilities"));

        return switch (type) {
            case "boolean" -> {
                double probability = answer.path("probability").asDouble();
                yield new JevResponse.DecisionResult(
                        name, String.valueOf(probability), probability, null, null);
            }
            case "choice" -> {
                String choice = answer.path("choice").asText();
                double confidence = probabilities != null
                        ? probabilities.getOrDefault(choice, 0.0)
                        : 0.0;
                yield new JevResponse.DecisionResult(name, choice, confidence, null, probabilities);
            }
            case "score" -> {
                double score = answer.path("score").asDouble();
                double confidence = probabilities == null ? 0.0
                        : probabilities.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                yield new JevResponse.DecisionResult(
                        name, String.valueOf(score), confidence, score, probabilities);
            }
            default -> new JevResponse.DecisionResult(name, answer.toString(), 0.0, null, null);
        };
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
