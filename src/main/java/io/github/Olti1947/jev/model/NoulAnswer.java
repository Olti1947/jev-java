package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Answer to a {@link Noul} question: the probability, 0 to 1, that the
 * answer is yes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NoulAnswer(double noul) implements JevAnswer {

    /**
     * Whether the yes-probability reaches the given threshold.
     */
    public boolean isTrue(double threshold) {
        return noul >= threshold;
    }
}
