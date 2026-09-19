package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.Olti1947.jev.exception.JevValidationException;

import java.util.Arrays;
import java.util.List;

/**
 * Score primitive rating the state along an ordered scale.
 *
 * <p>{@code criteria} is an ordered list of level descriptions, from the low end
 * of the scale to the high end. The position in the list is the level number:
 * the first entry is level 0, the second level 1, and so on. The API returns an
 * interpolated score across those levels (e.g. {@code 1.94}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Score(
        String name,
        String instructions,
        List<String> criteria
) implements JevPrimitive {

    public static final int MIN_LEVELS = 2;
    public static final int MAX_LEVELS = 10;

    public Score {
        if (name == null || name.isBlank()) {
            throw new JevValidationException("Score 'name' must not be blank.");
        }
        if (criteria == null || criteria.size() < MIN_LEVELS) {
            throw new JevValidationException(
                    "Score requires at least " + MIN_LEVELS + " ordered levels in 'criteria'.");
        }
        if (criteria.size() > MAX_LEVELS) {
            throw new JevValidationException(
                    "Score supports a maximum of " + MAX_LEVELS + " levels.");
        }
        if (criteria.stream().anyMatch(level -> level == null || level.isBlank())) {
            throw new JevValidationException("Score 'criteria' levels must not be blank.");
        }
        criteria = List.copyOf(criteria);
    }

    public Score(String name, String instructions, String... criteria) {
        this(name, instructions, criteria == null ? null : Arrays.asList(criteria));
    }
}
