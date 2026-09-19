package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.Olti1947.jev.exception.JevValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Choice primitive selecting one option from a predefined set.
 *
 * <p>{@code criteria} maps each option name to a description of when it
 * applies. A description may be {@code null} when the option name is
 * self-explanatory, a plain string, or a structured value: a {@link Rubric}
 * (or an equivalent map) with {@code what} / {@code not_for} / {@code examples},
 * or a nested map/list describing a hierarchy of categories. The API returns the
 * selected option together with a probability for every option.
 *
 * <p>{@code instructions} is either a plain string or a structured value such
 * as a map with {@code question} and {@code focus} keys.
 */
public record Choice(
        @JsonIgnore String name,
        Object instructions,
        Map<String, ?> criteria
) implements JevPrimitive {

    public static final int MAX_OPTIONS = 255;

    public Choice {
        if (name == null || name.isBlank()) {
            throw new JevValidationException("Choice 'name' must not be blank.");
        }
        if (criteria == null || criteria.isEmpty()) {
            throw new JevValidationException("Choice requires at least one option in 'criteria'.");
        }
        if (criteria.size() > MAX_OPTIONS) {
            throw new JevValidationException(
                    "Choice supports a maximum of " + MAX_OPTIONS + " options.");
        }
    }

    /**
     * Convenience constructor for options without descriptions.
     */
    public Choice(String name, Object instructions, List<String> options) {
        this(name, instructions, toCriteria(options));
    }

    private static Map<String, String> toCriteria(List<String> options) {
        if (options == null) {
            return null;
        }
        Map<String, String> criteria = new LinkedHashMap<>();
        options.forEach(option -> criteria.put(option, null));
        return criteria;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private Object instructions;
        private final Map<String, Object> criteria = new LinkedHashMap<>();

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
         * Adds an option without a description.
         */
        public Builder candidate(String option) {
            return candidate(option, null);
        }

        /**
         * Adds an option with a description: a string, a {@link Rubric}, or any
         * other structured value.
         */
        public Builder candidate(String option, Object description) {
            if (option == null || option.isBlank()) {
                throw new JevValidationException("Choice option name must not be blank.");
            }
            if (criteria.containsKey(option)) {
                throw new JevValidationException("Choice option '" + option + "' is already defined.");
            }
            criteria.put(option, description);
            return this;
        }

        public Choice build() {
            return new Choice(name, instructions, criteria);
        }
    }
}
