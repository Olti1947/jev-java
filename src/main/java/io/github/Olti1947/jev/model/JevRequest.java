package io.github.Olti1947.jev.model;

import io.github.Olti1947.jev.exception.JevValidationException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root request payload matching Jev's REST evaluation schema:
 * {@code {"state": ..., "model": ..., "questions": {"<name>": Question}}}.
 *
 * <p>Questions are keyed by name; the name is not part of the question object.
 *
 * <p>Every request is validated on construction, so both transports (native and
 * gateway) only ever see a well-formed request.
 */
public record JevRequest(
        String model,
        Object state,
        Map<String, JevPrimitive> questions
) {
    /**
     * @throws JevValidationException if {@code state} is null, there are no
     *         questions, a question is null, or a key differs from its question's name
     */
    public JevRequest {
        if (state == null) {
            throw new JevValidationException("JevRequest 'state' must not be null.");
        }
        if (questions == null || questions.isEmpty()) {
            throw new JevValidationException("JevRequest requires at least one primitive");
        }
        questions.forEach((name, primitive) -> {
            if (primitive == null) {
                throw new JevValidationException("Primitive '" + name + "' must not be null.");
            }
            if (!primitive.name().equals(name)) {
                throw new JevValidationException(
                        "Question key '" + name + "' does not match primitive name '" + primitive.name() + "'.");
            }
        });
        questions = Collections.unmodifiableMap(new LinkedHashMap<>(questions));
    }

    /**
     * @throws JevValidationException if {@code state} is null, {@code primitives}
     *         is null or empty, contains null, or two primitives share a name
     */
    public JevRequest(Object state, List<JevPrimitive> primitives) {
        this("jev-latest", state, toQuestions(primitives));
    }

    private static Map<String, JevPrimitive> toQuestions(List<JevPrimitive> primitives) {
        Map<String, JevPrimitive> questions = new LinkedHashMap<>();
        if (primitives != null) {
            for (JevPrimitive primitive : primitives) {
                if (primitive == null) {
                    throw new JevValidationException("Primitives must not contain null.");
                }
                if (questions.putIfAbsent(primitive.name(), primitive) != null) {
                    throw new JevValidationException("Duplicate primitive name: " + primitive.name());
                }
            }
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
         * @throws JevValidationException if {@code primitive} is null or a primitive
         *         with the same name was already added
         */
        public Builder addPrimitive(JevPrimitive primitive) {
            if (primitive == null) {
                throw new JevValidationException("Primitive must not be null.");
            }
            if (questions.containsKey(primitive.name())) {
                throw new JevValidationException("Duplicate primitive name: " + primitive.name());
            }
            questions.put(primitive.name(), primitive);
            return this;
        }

        /**
         * @throws JevValidationException if no state was set or no primitives were added
         */
        public JevRequest build() {
            return new JevRequest(model, state, questions);
        }
    }
}
