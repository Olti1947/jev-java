package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Answer to a {@link Choice} question: the selected option, the model's
 * confidence in it, and the probability of every option.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoiceAnswer(
        String choice,
        double confidence,
        Map<String, Double> probabilities
) implements JevAnswer {
}
