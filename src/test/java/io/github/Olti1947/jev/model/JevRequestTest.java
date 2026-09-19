package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the native request serialization against the documented schema
 * (https://docs.typesafe.ai/api): questions are a map keyed by name, the name
 * is not part of the question object, and each question carries its type.
 */
class JevRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesQuestionsAsMapKeyedByName() throws Exception {
        JevRequest request = new JevRequest(
                "My card was charged twice.",
                List.of(
                        new Choice("department", "Which team should handle this?",
                                Map.of("billing", "Charges, invoices, payment problems")),
                        new Noul("is_urgent", "Is this urgent?")));

        JsonNode json = mapper.valueToTree(request);

        assertEquals("jev-latest", json.get("model").asText());
        assertEquals("My card was charged twice.", json.get("state").asText());

        JsonNode department = json.get("questions").get("department");
        assertEquals("choice", department.get("type").asText());
        assertEquals("Which team should handle this?", department.get("instructions").asText());
        assertEquals("Charges, invoices, payment problems",
                department.get("criteria").get("billing").asText());
        assertFalse(department.has("name"), "name must not appear inside the question object");

        JsonNode urgent = json.get("questions").get("is_urgent");
        assertEquals("noul", urgent.get("type").asText());
        assertFalse(urgent.has("name"));
    }

    @Test
    void choiceFromOptionsListSerializesNullDescriptions() throws Exception {
        JevRequest request = new JevRequest("state",
                List.of(new Choice("intent", "Classify", List.of("billing", "support"))));

        JsonNode criteria = mapper.valueToTree(request)
                .get("questions").get("intent").get("criteria");
        assertTrue(criteria.has("billing"));
        assertTrue(criteria.get("billing").isNull(),
                "options without descriptions serialize as null per the schema");
    }

    @Test
    void scoreSerializesOrderedCriteriaArray() throws Exception {
        JevRequest request = new JevRequest("state",
                List.of(new Score("severity", "How severe?", "cosmetic", "degraded", "blocking")));

        JsonNode question = mapper.valueToTree(request).get("questions").get("severity");
        assertEquals("score", question.get("type").asText());
        assertTrue(question.get("criteria").isArray());
        assertEquals("cosmetic", question.get("criteria").get(0).asText());
        assertFalse(question.has("name"));
    }
}
