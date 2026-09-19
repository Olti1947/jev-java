package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.Olti1947.jev.exception.JevValidationException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Structured description of one {@link Score} level: a short summary plus the
 * signals that indicate the level.
 *
 * <p>Serializes to {@code {"summary": ..., "signals": [...]}}; unset fields are
 * omitted. At least a summary or one signal is required.
 *
 * @param summary brief description of the level
 * @param signals indicators that the state belongs at this level
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScoreLevel(
        String summary,
        List<String> signals
) {

    public ScoreLevel {
        boolean hasSummary = summary != null && !summary.isBlank();
        boolean hasSignals = signals != null && !signals.isEmpty();
        if (!hasSummary && !hasSignals) {
            throw new JevValidationException("ScoreLevel requires a 'summary' or at least one signal.");
        }
        if (signals != null) {
            if (signals.stream().anyMatch(signal -> signal == null || signal.isBlank())) {
                throw new JevValidationException("ScoreLevel 'signals' must not be blank.");
            }
            signals = List.copyOf(signals);
        }
    }

    public static ScoreLevel of(String summary, String... signals) {
        return new ScoreLevel(summary, signals == null || signals.length == 0 ? null : List.of(signals));
    }

    /**
     * Reduces any form of level to a one-line description: a string is returned
     * as is, a structured level (a {@code ScoreLevel} or an equivalent map) is
     * described by its summary, falling back to its signals joined by "; ".
     */
    public static String describe(Object level) {
        if (level instanceof ScoreLevel scoreLevel) {
            return describe(scoreLevel.summary(), scoreLevel.signals());
        }
        if (level instanceof Map<?, ?> map) {
            return describe(map.get("summary"), map.get("signals"));
        }
        return String.valueOf(level);
    }

    private static String describe(Object summary, Object signals) {
        if (summary instanceof String text && !text.isBlank()) {
            return text;
        }
        if (signals instanceof Collection<?> items && !items.isEmpty()) {
            return items.stream().map(String::valueOf).collect(Collectors.joining("; "));
        }
        return String.valueOf(summary);
    }
}
