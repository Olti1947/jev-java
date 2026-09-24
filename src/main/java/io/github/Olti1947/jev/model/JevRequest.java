package io.github.Olti1947.jev.model;

import io.github.Olti1947.jev.exception.JevValidationException;

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

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link JevRequest}, so a request can be assembled in one
     * place (e.g. adding primitives conditionally) and executed in another via
     * {@code JevClient.evaluate(JevRequest)} / {@code evaluateAsync(JevRequest)}.
     */
    public static final class Builder {
        private String model = "jev-latest";
        private Object state;
        private final Map<String, JevPrimitive> questions = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder state(Object state) {
            this.state = state;
            return this;
        }

        /**
         * Adds a primitive to the request.
         *
         * @throws JevValidationException if a primitive with the same name was already added
         */
        public Builder addPrimitive(JevPrimitive primitive) {
            if (questions.containsKey(primitive.name())) {
                throw new JevValidationException("Duplicate primitive name: " + primitive.name());
            }
            questions.put(primitive.name(), primitive);
            return this;
        }

        /**
         * @throws JevValidationException if no primitives were added
         */
        public JevRequest build() {
            if (questions.isEmpty()) {
                throw new JevValidationException("JevRequest requires at least one primitive");
            }
            return new JevRequest(model, state, new LinkedHashMap<>(questions));
        }
    }
}
