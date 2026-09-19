package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Response returned by Jev, matching the documented schema:
 * {@code {"model": ..., "answers": {"<name>": Answer}, "usage": {...}}}.
 * Answers are keyed by the question names from the request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JevResponse(
        String model,
        Map<String, JevAnswer> answers,
        Usage usage
) {
    /**
     * The answer for the given question name, or {@code null} if absent.
     */
    public JevAnswer answer(String name) {
        return answers == null ? null : answers.get(name);
    }

    public NoulAnswer noul(String name) {
        return (NoulAnswer) answer(name);
    }

    public ChoiceAnswer choice(String name) {
        return (ChoiceAnswer) answer(name);
    }

    public ScoreAnswer score(String name) {
        return (ScoreAnswer) answer(name);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("input_tokens") long inputTokens,
            @JsonProperty("output_tokens") long outputTokens
    ) {
    }
}
