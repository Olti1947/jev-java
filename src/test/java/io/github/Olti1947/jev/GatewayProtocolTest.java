package io.github.Olti1947.jev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.Olti1947.jev.model.Choice;
import io.github.Olti1947.jev.model.JevPrimitive;
import io.github.Olti1947.jev.model.JevResponse;
import io.github.Olti1947.jev.model.Noul;
import io.github.Olti1947.jev.model.Score;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayProtocolTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildBodyMapsNoulToBooleanQuestion() {
        ObjectNode body = GatewayProtocol.buildBody(mapper, "some state",
                List.of(new Noul("is_angry", "Is the customer angry?")));

        JsonNode question = body.get("questions").get("is_angry");
        assertEquals("boolean", question.get("type").asText());
        assertEquals("Is the customer angry?", question.get("instructions").asText());
        assertFalse(question.has("criteria"));
    }

    @Test
    void buildBodyMapsChoiceOptionsToCriteria() {
        ObjectNode body = GatewayProtocol.buildBody(mapper, "state",
                List.of(new Choice("intent", "Classify", List.of("billing", "support"))));

        JsonNode criteria = body.get("questions").get("intent").get("criteria");
        assertEquals("billing", criteria.get("billing").asText());
        assertEquals("support", criteria.get("support").asText());
    }

    @Test
    void buildBodyPrefersExplicitChoiceCriteria() {
        Choice choice = new Choice("intent", "Classify", List.of("billing"),
                Map.of("billing", "payment problems"));
        ObjectNode body = GatewayProtocol.buildBody(mapper, "state", List.of(choice));

        assertEquals("payment problems",
                body.get("questions").get("intent").get("criteria").get("billing").asText());
    }

    @Test
    void buildBodyMapsScoreCriteriaToOrderedArray() {
        ObjectNode body = GatewayProtocol.buildBody(mapper, "state",
                List.of(new Score("mood", "Rate the mood",
                        List.of("no frustration", "strong frustration"))));

        JsonNode criteria = body.get("questions").get("mood").get("criteria");
        assertTrue(criteria.isArray());
        assertEquals("no frustration", criteria.get(0).asText());
        assertEquals("strong frustration", criteria.get(1).asText());
    }

    @Test
    void parseResponseMapsAnswersInRequestOrder() throws Exception {
        String json = """
                {
                  "answers": {
                    "mood": {"type": "score", "score": 1.9, "probabilities": {"0": 0.0, "1": 0.1, "2": 0.9}},
                    "is_angry": {"type": "boolean", "probability": 0.97},
                    "intent": {"type": "choice", "choice": "billing",
                               "probabilities": {"billing": 0.98, "support": 0.02}}
                  },
                  "providerMetadata": {"gateway": {"generationId": "gen_123"}}
                }
                """;
        List<JevPrimitive> primitives = List.of(
                new Noul("is_angry", "angry?"),
                new Choice("intent", "classify", List.of("billing", "support")),
                new Score("mood", "rate", "calm", "angry"));

        JevResponse response = GatewayProtocol.parseResponse(mapper.readTree(json), primitives, 123);

        assertEquals("gen_123", response.id());
        assertEquals(123, response.latencyMs());
        assertEquals(3, response.results().size());

        JevResponse.DecisionResult angry = response.results().get(0);
        assertEquals("is_angry", angry.name());
        assertEquals(0.97, angry.confidence());
        assertTrue(angry.isTrue(0.9));

        JevResponse.DecisionResult intent = response.results().get(1);
        assertEquals("billing", intent.value());
        assertEquals(0.98, intent.confidence());
        assertEquals(0.02, intent.probabilities().get("support"));

        JevResponse.DecisionResult mood = response.results().get(2);
        assertEquals(1.9, mood.score());
        assertEquals(0.9, mood.confidence());
    }

    @Test
    void parseResponseSkipsMissingAnswersAndHandlesAbsentMetadata() throws Exception {
        String json = """
                {"answers": {"is_angry": {"type": "boolean", "probability": 0.5}}}
                """;
        List<JevPrimitive> primitives = List.of(
                new Noul("is_angry", "angry?"),
                new Noul("not_answered", "missing?"));

        JevResponse response = GatewayProtocol.parseResponse(mapper.readTree(json), primitives, 1);

        assertNull(response.id());
        assertEquals(1, response.results().size());
        assertEquals("is_angry", response.results().get(0).name());
    }
}
