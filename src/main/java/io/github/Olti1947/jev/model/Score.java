package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.Olti1947.jev.exception.JevValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Score primitive rating the state along an ordered scale.
 *
 * <p>{@code criteria} is an ordered list of level descriptions, from the low end
 * of the scale to the high end. The position in the list is the level number:
 * the first entry is level 0, the second level 1, and so on. Each level is a
 * plain string or a structured value such as a {@link ScoreLevel} (or an
 * equivalent map) with a {@code summary} and a list of {@code signals}. The API
 * returns an interpolated score across those levels (e.g. {@code 1.94}).
 *
 * <p>{@code instructions} is either a plain string or a structured value such
 * as a map with {@code question} and {@code note} keys.
 */
public record Score(
        @JsonIgnore String name,
        Object instructions,
        List<?> criteria
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
        if (criteria.stream().anyMatch(Score::isBlankLevel)) {
            throw new JevValidationException("Score 'criteria' levels must not be blank.");
        }
        criteria = List.copyOf(criteria);
    }

    public Score(String name, Object instructions, String... criteria) {
        this(name, instructions, criteria == null ? null : Arrays.asList(criteria));
    }

    private static boolean isBlankLevel(Object level) {
        if (level == null) {
            return true;
        }
        if (level instanceof CharSequence text) {
            return text.toString().isBlank();
        }
        if (level instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        if (level instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private Object instructions;
        private final List<Object> criteria = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
        }

        /**
         * Sets the instructions: a plain string or a structured value (e.g. a map).
         */
        public Builder instructions(Object instructions) {
            this.instructions = instructions;
            return this;
        }

        /**
         * Appends the next level, in order from the low end of the scale to the
         * high end: a string, a {@link ScoreLevel}, or any other structured value.
         */
        public Builder addCriteriaLevel(Object level) {
            criteria.add(level);
            return this;
        }

        public Score build() {
            return new Score(name, instructions, criteria);
        }
    }
}
