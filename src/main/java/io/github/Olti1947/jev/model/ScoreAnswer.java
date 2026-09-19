package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

/**
 * Answer to a {@link Score} question: the interpolated position on the
 * scale, the confidence, the probability of each level (keyed by level
 * number), and the legend mapping level numbers back to descriptions.
 *
 * <p>For levels sent as structured values, the legend holds the level's
 * summary (or its signals when there is no summary).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoreAnswer(
        double score,
        double confidence,
        Map<String, Double> probabilities,
        @JsonDeserialize(contentUsing = LegendEntryDeserializer.class) Map<String, String> legend
) implements JevAnswer {
}
