package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.Olti1947.jev.exception.JevValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Noul primitive for binary (Yes/No / True/False) evaluation.
 *
 * <p>{@code instructions} is either a plain string or a structured value such
 * as a map with {@code question}, {@code focus}, {@code inspect} or
 * {@code field} keys. {@code criteria} maps {@code "true"} and {@code "false"}
 * to a description of that state: a string or a structured value such as a
 * {@link Rubric}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Noul(
        @JsonIgnore String name,
        Object instructions,
        Map<String, ?> criteria
) implements JevPrimitive {

    public Noul {
        if (name == null || name.isBlank()) {
            throw new JevValidationException("Noul 'name' must not be blank.");
        }
    }

    public Noul(String name, Object instructions) {
        this(name, instructions, null);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private Object instructions;
        private Object whenTrue;
        private Object whenFalse;

        private Builder(String name) {
            this.name = name;
        }

        /**
         * Sets the instructions: a plain string or a structured value (e.g. a map).
         */
        public Builder instructions(Object instructions) {
            this.instructions = instructions;
            return this;
        }

        /**
         * Describes the {@code true} and {@code false} states, each a string or
         * a structured value such as a {@link Rubric}. Either may be {@code null}.
         */
        public Builder criteria(Object whenTrue, Object whenFalse) {
            this.whenTrue = whenTrue;
            this.whenFalse = whenFalse;
            return this;
        }

        public Noul build() {
            Map<String, Object> criteria = null;
            if (whenTrue != null || whenFalse != null) {
                criteria = new LinkedHashMap<>();
                if (whenTrue != null) {
                    criteria.put("true", whenTrue);
                }
                if (whenFalse != null) {
                    criteria.put("false", whenFalse);
                }
            }
            return new Noul(name, instructions, criteria);
        }
    }
}
