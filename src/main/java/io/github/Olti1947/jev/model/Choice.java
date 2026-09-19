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
 * self-explanatory. The API returns the selected option together with a
 * probability for every option.
 */
public record Choice(
        @JsonIgnore String name,
        String instructions,
        Map<String, String> criteria
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
    public Choice(String name, String instructions, List<String> options) {
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
}
