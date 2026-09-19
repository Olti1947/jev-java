package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Answer to a {@link Score} question: the interpolated position on the
 * scale, the confidence, the probability of each level (keyed by level
 * number), and the legend mapping level numbers back to descriptions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoreAnswer(
        double score,
        double confidence,
        Map<String, Double> probabilities,
        Map<String, String> legend
) implements JevAnswer {
}
