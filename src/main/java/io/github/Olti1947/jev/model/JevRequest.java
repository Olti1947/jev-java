package io.github.Olti1947.jev.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root request payload matching Jev's REST evaluation schema:
 * {@code {"state": ..., "model": ..., "questions": {"<name>": Question}}}.
 *
 * <p>Questions are keyed by name; the name is not part of the question object.
 */
public record JevRequest(
        String model,
        Object state,
        Map<String, JevPrimitive> questions
) {
    public JevRequest(Object state, List<JevPrimitive> primitives) {
        this("jev-latest", state, toQuestions(primitives));
    }

    private static Map<String, JevPrimitive> toQuestions(List<JevPrimitive> primitives) {
        Map<String, JevPrimitive> questions = new LinkedHashMap<>();
        if (primitives != null) {
            primitives.forEach(primitive -> questions.put(primitive.name(), primitive));
        }
        return questions;
    }
}
