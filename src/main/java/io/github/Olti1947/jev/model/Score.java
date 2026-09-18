package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.Olti1947.jev.exception.JevValidationException;

import java.util.Map;

/**
 * Score primitive for evaluating continuous ranges or rubric levels.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Score(
        String name,
        String instructions,
        Double min,
        Double max,
        Map<String, String> legend
) implements JevPrimitive {

    public Score {
        if (name == null || name.isBlank()) {
            throw new JevValidationException("Score 'name' must not be blank.");
        }
    }

    public Score(String name, String instructions, double min, double max) {
        this(name, instructions, min, max, null);
    }

    public Score(String name, String instructions, Map<String, String> legend) {
        this(name, instructions, null, null, legend);
    }
}