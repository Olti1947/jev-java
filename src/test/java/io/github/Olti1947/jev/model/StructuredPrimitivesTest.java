package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Olti1947.jev.exception.JevValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structured instructions and criteria (https://docs.typesafe.ai/primitives/advanced):
 * every field accepts a plain string or a structured object/array alongside the
 * original string forms.
 */
class StructuredPrimitivesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode question(JevPrimitive primitive) {
        return mapper.valueToTree(new JevRequest("state", List.of(primitive)))
                .get("questions").get(primitive.name());
    }

    // --- instructions ---------------------------------------------------

    @Test
    void noulSerializesStructuredInstructions() {
        Noul invoiceCheck = Noul.builder("invoice_number_is_correct")
                .instructions(Map.of(
                        "field", Map.of(
                                "name", "invoice_number",
                                "type", "string",
                                "description", "The identifier printed on the invoice."),
                        "extracted_value", "4471",
                        "question", "Does `extracted_value` match the `field` as it appears in `source_text`?"))
                .build();

        JsonNode instructions = question(invoiceCheck).get("instructions");
        assertTrue(instructions.isObject());
        assertEquals("invoice_number", instructions.get("field").get("name").asText());
        assertEquals("4471", instructions.get("extracted_value").asText());
        assertTrue(instructions.get("question").asText().startsWith("Does `extracted_value`"));
    }

    @Test
    void stringInstructionsStillSerializeAsStrings() {
        JsonNode instructions = question(new Noul("is_urgent", "Is this urgent?")).get("instructions");
        assertTrue(instructions.isTextual());
        assertEquals("Is this urgent?", instructions.asText());
    }

    @Test
    void nullInstructionsAreOmitted() {
        assertFalse(question(Noul.builder("is_urgent").build()).has("instructions"));
    }

    // --- Choice ---------------------------------------------------------

    @Test
    void choiceSerializesRubricCriteriaFromMapsAndTypedRubrics() {
        Choice department = Choice.builder("department")
                .instructions(Map.of(
                        "question", "Which team should handle this message?",
                        "focus", "Classify the customer's primary request, not every topic mentioned."))
                .candidate("billing", Map.of(
                        "what", "Charges, invoices, refunds, or subscriptions",
                        "not_for", "Order tracking or account access",
                        "examples", List.of("I was charged twice", "Where is my refund?")))
                .candidate("orders", Rubric.of("Order status, delivery, cancellation, or returns",
                                "Where is my package?", "Cancel my order")
                        .notFor("Charges or account access"))
                .build();

        JsonNode json = question(department);
        assertEquals("Classify the customer's primary request, not every topic mentioned.",
                json.get("instructions").get("focus").asText());

        JsonNode billing = json.get("criteria").get("billing");
        assertEquals("Order tracking or account access", billing.get("not_for").asText());
        assertEquals("Where is my refund?", billing.get("examples").get(1).asText());

        JsonNode orders = json.get("criteria").get("orders");
        assertEquals("Order status, delivery, cancellation, or returns", orders.get("what").asText());
        assertEquals("Charges or account access", orders.get("not_for").asText(),
                "Rubric.notFor serializes under the snake_case wire name");
        assertFalse(orders.has("notFor"));
        assertEquals(2, orders.get("examples").size());
    }

    @Test
    void choiceSerializesNestedCategoryMaps() {
        Choice choice = Choice.builder("topic")
                .instructions("Pick the leaf topic.")
                .candidate("sales", Map.of(
                        "enterprise", List.of("demo_request", "pricing"),
                        "self_serve", Map.of("leaf", "value")))
                .candidate("support")
                .build();

        JsonNode criteria = question(choice).get("criteria");
        assertEquals("pricing", criteria.get("sales").get("enterprise").get(1).asText());
        assertEquals("value", criteria.get("sales").get("self_serve").get("leaf").asText());
        assertTrue(criteria.get("support").isNull(), "options without a description stay null");
    }

    @Test
    void rubricOmitsUnsetFields() {
        JsonNode rubric = mapper.valueToTree(Rubric.of("Charges or refunds"));
        assertEquals("Charges or refunds", rubric.get("what").asText());
        assertFalse(rubric.has("not_for"));
        assertFalse(rubric.has("examples"));
    }

    @Test
    void choiceBuilderRejectsDuplicateAndBlankOptions() {
        Choice.Builder builder = Choice.builder("department").candidate("billing");
        assertThrows(JevValidationException.class, () -> builder.candidate("billing"));
        assertThrows(JevValidationException.class, () -> builder.candidate(" "));
    }

    @Test
    void choiceBuilderRequiresAtLeastOneCandidate() {
        assertThrows(JevValidationException.class, () -> Choice.builder("department").build());
    }

    @Test
    void choiceStillAcceptsPlainStringMapAndOptionList() {
        Map<String, String> described = Map.of("billing", "payment problems");
        Choice fromMap = new Choice("intent", "Classify", described);
        assertEquals("payment problems", question(fromMap).get("criteria").get("billing").asText());

        Choice fromList = new Choice("intent", "Classify", List.of("billing", "support"));
        assertTrue(question(fromList).get("criteria").get("support").isNull());
    }

    // --- Score ----------------------------------------------------------

    @Test
    void scoreSerializesStructuredLevels() {
        Score prScope = Score.builder("pr_scope")
                .instructions(Map.of(
                        "question", "How focused is this pull request description on a single change?",
                        "note", "Judge the number of independent changes, not the size of any one change."))
                .addCriteriaLevel(Map.of(
                        "summary", "One change, clearly stated",
                        "signals", List.of("A single fix or feature")))
                .addCriteriaLevel(ScoreLevel.of("Several independent changes bundled together",
                        "Two or more unrelated fixes or features",
                        "Changes that could each be their own PR"))
                .build();

        JsonNode json = question(prScope);
        assertEquals("score", json.get("type").asText());
        assertTrue(json.get("instructions").has("note"));

        JsonNode criteria = json.get("criteria");
        assertTrue(criteria.isArray());
        assertEquals("One change, clearly stated", criteria.get(0).get("summary").asText());
        assertEquals("A single fix or feature", criteria.get(0).get("signals").get(0).asText());
        assertEquals("Several independent changes bundled together", criteria.get(1).get("summary").asText());
        assertEquals(2, criteria.get(1).get("signals").size());
    }

    @Test
    void scoreMixesStringAndStructuredLevels() {
        Score score = Score.builder("severity")
                .instructions("How severe?")
                .addCriteriaLevel("cosmetic")
                .addCriteriaLevel(ScoreLevel.of("blocking", "users cannot continue"))
                .build();

        JsonNode criteria = question(score).get("criteria");
        assertEquals("cosmetic", criteria.get(0).asText());
        assertEquals("blocking", criteria.get(1).get("summary").asText());
    }

    @Test
    void scoreValidatesStructuredLevels() {
        assertThrows(JevValidationException.class, () -> Score.builder("s")
                .addCriteriaLevel("low").addCriteriaLevel(Map.of()).build());
        assertThrows(JevValidationException.class, () -> Score.builder("s")
                .addCriteriaLevel("low").addCriteriaLevel(null).build());
        assertThrows(JevValidationException.class, () -> Score.builder("s")
                .addCriteriaLevel("only one").build());
        assertThrows(JevValidationException.class, () -> new ScoreLevel(null, null));
        assertThrows(JevValidationException.class, () -> new ScoreLevel("summary", List.of(" ")));
    }

    /**
     * Captured from the live API: the legend echoes each level as it was sent,
     * so structured levels come back as objects.
     */
    @Test
    void scoreResponseWithStructuredLegendDeserializes() throws Exception {
        String json = """
                {"model":"jev-1.13.0","answers":{
                  "pr_scope":{"type":"score","score":1.0,"confidence":1.0,
                    "legend":{"0":{"summary":"One change, clearly stated","signals":["A single fix or feature"]},
                              "1":{"summary":"Several bundled changes","signals":["Two or more unrelated fixes"]}},
                    "probabilities":{"0":0.0,"1":1.0}},
                  "mixed":{"type":"score","score":0.98,"confidence":0.97,
                    "legend":{"0":"cosmetic","1":{"summary":"blocking","signals":["users cannot continue"]},
                              "2":{"signals":["a","b"]}},
                    "probabilities":{"0":0.02,"1":0.98}}},
                 "usage":{"input_tokens":401,"output_tokens":31}}
                """;
        ObjectMapper lenient = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JevResponse response = lenient.readValue(json, JevResponse.class);

        Map<String, String> structured = response.score("pr_scope").legend();
        assertEquals("One change, clearly stated", structured.get("0"));
        assertEquals("Several bundled changes", structured.get("1"));

        Map<String, String> mixed = response.score("mixed").legend();
        assertEquals("cosmetic", mixed.get("0"), "string levels are unchanged");
        assertEquals("blocking", mixed.get("1"));
        assertEquals("a; b", mixed.get("2"), "falls back to the signals without a summary");
    }

    // --- Noul -----------------------------------------------------------

    @Test
    void noulSerializesTrueAndFalseBoundaries() {
        Noul credentialCheck = Noul.builder("requests_credentials")
                .instructions(Map.of(
                        "question", "Does the `message` ask the recipient to disclose a sensitive credential?",
                        "inspect", "message",
                        "focus", "Look for a request to send the credential itself, not a request to change or reset it."))
                .criteria(
                        Map.of("what", "Asks the recipient to reply with, type, or send a password, PIN, or one-time code",
                                "examples", List.of("Reply with your password", "Send us the 6-digit code")),
                        Rubric.of("No sensitive credential is requested",
                                "Reset your password from the settings page", "Your statement is ready"))
                .build();

        JsonNode json = question(credentialCheck);
        assertEquals("noul", json.get("type").asText());
        assertEquals("message", json.get("instructions").get("inspect").asText());

        JsonNode criteria = json.get("criteria");
        assertEquals("Send us the 6-digit code", criteria.get("true").get("examples").get(1).asText());
        assertEquals("No sensitive credential is requested", criteria.get("false").get("what").asText());
        assertEquals(2, criteria.get("false").get("examples").size());
    }

    @Test
    void noulCriteriaOmitsUnsetStates() {
        Noul onlyTrue = Noul.builder("n").criteria("The sky is blue", null).build();
        JsonNode criteria = question(onlyTrue).get("criteria");
        assertEquals("The sky is blue", criteria.get("true").asText());
        assertFalse(criteria.has("false"));

        assertNull(Noul.builder("n").criteria(null, null).build().criteria());
    }

    @Test
    void noulStillAcceptsPlainStringCriteria() {
        Map<String, String> criteria = Map.of("true", "angry", "false", "calm");
        JsonNode json = question(new Noul("is_angry", "Is the customer angry?", criteria));
        assertEquals("angry", json.get("criteria").get("true").asText());
    }

    @Test
    void noulRequiresName() {
        assertThrows(JevValidationException.class, () -> Noul.builder(" ").build());
    }

    @Test
    void rubricRequiresWhatOrExamples() {
        assertThrows(JevValidationException.class, () -> new Rubric(null, "not for", null));
        assertThrows(JevValidationException.class, () -> new Rubric(" ", null, List.of()));
        assertThrows(JevValidationException.class, () -> new Rubric("what", null, List.of(" ")));
    }
}
