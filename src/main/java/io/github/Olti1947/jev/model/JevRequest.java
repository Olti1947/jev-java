package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Root request payload matching Jev's REST evaluation schema.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JevRequest(
        String model,
        Object state,
        List<JevPrimitive> evaluations
) {
    public JevRequest(Object state, List<JevPrimitive> evaluations) {
        this("jev-latest", state, evaluations);
    }
}