package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies response deserialization against the documented response schema
 * (https://docs.typesafe.ai/api): {model, answers keyed by question name, usage}.
 */
class JevResponseTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void deserializesDocumentedResponse() throws Exception {
        String json = """
                {
                  "model": "jev-latest",
                  "answers": {
                    "is_urgent": { "type": "noul", "noul": 0.92 },
                    "department": {
                      "type": "choice",
                      "choice": "billing",
                      "confidence": 0.97,
                      "probabilities": { "billing": 0.97, "shipping": 0.03 }
                    },
                    "severity": {
                      "type": "score",
                      "score": 1.94,
                      "confidence": 0.94,
                      "probabilities": { "0": 0.0, "1": 0.06, "2": 0.94 },
                      "legend": { "0": "cosmetic", "1": "degraded", "2": "blocking" }
                    }
                  },
                  "usage": { "input_tokens": 312, "output_tokens": 48 }
                }
                """;

        JevResponse response = mapper.readValue(json, JevResponse.class);

        assertEquals("jev-latest", response.model());
        assertEquals(312, response.usage().inputTokens());
        assertEquals(48, response.usage().outputTokens());

        NoulAnswer urgent = response.noul("is_urgent");
        assertEquals(0.92, urgent.noul());
        assertTrue(urgent.isTrue(0.9));

        ChoiceAnswer department = response.choice("department");
        assertEquals("billing", department.choice());
        assertEquals(0.97, department.confidence());
        assertEquals(0.03, department.probabilities().get("shipping"));

        ScoreAnswer severity = response.score("severity");
        assertEquals(1.94, severity.score());
        assertEquals("blocking", severity.legend().get("2"));
    }
}
