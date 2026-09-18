package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Structured response container returned by Jev.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JevResponse(
        String id,
        List<DecisionResult> results,
        @JsonProperty("latency_ms") long latencyMs
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DecisionResult(
            String name,
            String value,
            double confidence,
            Double score,
            Map<String, Double> probabilities
    ) {
        /**
         * Convenience helper to convert value or confidence into a double probability.
         */
        public double asProbability() {
            if (value != null) {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException ignored) {}
            }
            return confidence;
        }

        /**
         * Checks if a binary/noul evaluation passes a given threshold.
         */
        public boolean isTrue(double threshold) {
            return asProbability() >= threshold;
        }
    }
}