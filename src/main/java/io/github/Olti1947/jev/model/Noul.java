package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.Olti1947.jev.exception.JevValidationException;

import java.util.Map;

/**
 * Noul primitive for binary (Yes/No / True/False) evaluation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Noul(
        @JsonIgnore String name,
        String instructions,
        Map<String, String> criteria
) implements JevPrimitive {

    public Noul {
        if (name == null || name.isBlank()) {
            throw new JevValidationException("Noul 'name' must not be blank.");
        }
    }

    public Noul(String name, String instructions) {
        this(name, instructions, null);
    }
}