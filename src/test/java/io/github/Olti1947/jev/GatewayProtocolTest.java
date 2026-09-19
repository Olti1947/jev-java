package io.github.Olti1947.jev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.Olti1947.jev.model.Choice;
import io.github.Olti1947.jev.model.ChoiceAnswer;
import io.github.Olti1947.jev.model.JevPrimitive;
import io.github.Olti1947.jev.model.JevResponse;
import io.github.Olti1947.jev.model.Noul;
import io.github.Olti1947.jev.model.NoulAnswer;
import io.github.Olti1947.jev.model.Rubric;
import io.github.Olti1947.jev.model.Score;
import io.github.Olti1947.jev.model.ScoreAnswer;
import io.github.Olti1947.jev.model.ScoreLevel;
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
        assertFalse(question.has("name"));
    }

    @Test
    void buildBodyKeepsChoiceCriteria() {
        ObjectNode body = GatewayProtocol.buildBody(mapper, "state",
                List.of(new Choice("intent", "Classify",
                        Map.of("billing", "payment problems"))));

        JsonNode question = body.get("questions").get("intent");
        assertEquals("choice", question.get("type").asText());
        assertEquals("payment problems", question.get("criteria").get("billing").asText());
    }

    @Test
    void buildBodyKeepsScoreCriteriaOrder() {
        ObjectNode body = GatewayProtocol.buildBody(mapper, "state",
                List.of(new Score("mood", "Rate the mood",
                        "no frustration", "strong frustration")));

        JsonNode criteria = body.get("questions").get("mood").get("criteria");
        assertTrue(criteria.isArray());
        assertEquals("no frustration", criteria.get(0).asText());
        assertEquals("strong frustration", criteria.get(1).asText());
    }

    @Test
    void parseResponseMapsGatewayAnswersToNativeShape() throws Exception {
        String json = """
                {
                  "answers": {
                    "mood": {"type": "score", "score": 1.9, "probabilities": {"0": 0.0, "1": 0.1, "2": 0.9}},
                    "is_angry": {"type": "boolean", "probability": 0.97},
                    "intent": {"type": "choice", "choice": "billing",
                               "probabilities": {"billing": 0.98, "support": 0.02}}
                  },
                  "usage": {"inputTokens": 522, "outputTokens": 92},
                  "providerMetadata": {"typesafe": {"confidence": {"intent": 1.0}}}
                }
                """;
        List<JevPrimitive> primitives = List.of(
                new Noul("is_angry", "angry?"),
                new Choice("intent", "classify", List.of("billing", "support")),
                new Score("mood", "rate", "calm", "annoyed", "angry"));

        JevResponse response = GatewayProtocol.parseResponse(
                mapper.readTree(json), primitives, "typesafe-ai/jev");

        assertEquals("typesafe-ai/jev", response.model());
        assertEquals(522, response.usage().inputTokens());
        assertEquals(92, response.usage().outputTokens());

        NoulAnswer angry = response.noul("is_angry");
        assertEquals(0.97, angry.noul());
        assertTrue(angry.isTrue(0.9));

        ChoiceAnswer intent = response.choice("intent");
        assertEquals("billing", intent.choice());
        assertEquals(1.0, intent.confidence(), "confidence comes from provider metadata");
        assertEquals(0.02, intent.probabilities().get("support"));

        ScoreAnswer mood = response.score("mood");
        assertEquals(1.9, mood.score());
        assertEquals(0.9, mood.confidence(), "falls back to the top level probability");
        assertEquals("angry", mood.legend().get("2"), "legend is rebuilt from the request criteria");
    }

    @Test
    void parseResponseDescribesStructuredScoreLevelsInLegend() throws Exception {
        String json = """
                {"answers": {"mood": {"type": "score", "score": 1.0,
                                      "probabilities": {"0": 0.1, "1": 0.8, "2": 0.1}}}}
                """;
        JevPrimitive mood = Score.builder("mood")
                .instructions("rate")
                .addCriteriaLevel("calm")
                .addCriteriaLevel(new ScoreLevel("annoyed", List.of("short replies")))
                .addCriteriaLevel(Map.of("signals", List.of("shouting", "threats")))
                .build();

        JevResponse response = GatewayProtocol.parseResponse(
                mapper.readTree(json), List.of(mood), "typesafe-ai/jev");

        Map<String, String> legend = response.score("mood").legend();
        assertEquals("calm", legend.get("0"));
        assertEquals("annoyed", legend.get("1"), "structured levels are described by their summary");
        assertEquals("shouting; threats", legend.get("2"), "falls back to the signals without a summary");
    }

    @Test
    void buildBodyKeepsStructuredCriteria() {
        ObjectNode body = GatewayProtocol.buildBody(mapper, "state", List.of(
                Noul.builder("is_angry")
                        .instructions(Map.of("question", "Is the customer angry?"))
                        .criteria(Rubric.of("Clearly angry"), Rubric.of("Calm"))
                        .build()));

        JsonNode question = body.get("questions").get("is_angry");
        assertEquals("boolean", question.get("type").asText());
        assertEquals("Is the customer angry?", question.get("instructions").get("question").asText());
        assertEquals("Clearly angry", question.get("criteria").get("true").get("what").asText());
    }

    @Test
    void parseResponseSkipsMissingAnswers() throws Exception {
        String json = """
                {"answers": {"is_angry": {"type": "boolean", "probability": 0.5}}}
                """;
        List<JevPrimitive> primitives = List.of(
                new Noul("is_angry", "angry?"),
                new Noul("not_answered", "missing?"));

        JevResponse response = GatewayProtocol.parseResponse(
                mapper.readTree(json), primitives, "typesafe-ai/jev");

        assertEquals(1, response.answers().size());
        assertNull(response.answer("not_answered"));
    }
}
