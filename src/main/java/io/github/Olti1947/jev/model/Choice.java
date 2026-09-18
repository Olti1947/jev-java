package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.Olti1947.jev.exception.JevValidationException;

import java.util.List;
import java.util.Map;

/**
 * Choice primitive for multi-class classification from a set of predefined options.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Choice(
        String name,
        String instructions,
        List<String> options,
        Map<String, String> criteria
) implements JevPrimitive {

    public Choice {
        if (name == null || name.isBlank()) {
            throw new JevValidationException("Choice 'name' must not be blank.");
        }
        if (options == null || options.isEmpty()) {
            throw new JevValidationException("Choice primitive requires at least one option.");
        }
        if (options.size() > 255) {
            throw new JevValidationException("Choice primitive supports a maximum of 255 options.");
        }
    }

    public Choice(String name, String instructions, List<String> options) {
        this(name, instructions, options, null);
    }
}