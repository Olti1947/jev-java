package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.Olti1947.jev.exception.JevValidationException;

import java.util.List;

/**
 * Structured description of a {@link Choice} option or of the {@code true} /
 * {@code false} state of a {@link Noul}, with explicit boundaries that help
 * disambiguate close calls.
 *
 * <p>Serializes to {@code {"what": ..., "not_for": ..., "examples": [...]}};
 * unset fields are omitted. At least {@code what} or one example is required.
 *
 * @param what     what the option or state covers
 * @param notFor   related cases it must not be used for
 * @param examples sample inputs that belong here
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Rubric(
        String what,
        @JsonProperty("not_for") String notFor,
        List<String> examples
) {

    public Rubric {
        boolean hasWhat = what != null && !what.isBlank();
        boolean hasExamples = examples != null && !examples.isEmpty();
        if (!hasWhat && !hasExamples) {
            throw new JevValidationException("Rubric requires 'what' or at least one example.");
        }
        if (examples != null) {
            if (examples.stream().anyMatch(example -> example == null || example.isBlank())) {
                throw new JevValidationException("Rubric 'examples' must not be blank.");
            }
            examples = List.copyOf(examples);
        }
    }

    public static Rubric of(String what) {
        return new Rubric(what, null, null);
    }

    public static Rubric of(String what, String... examples) {
        return new Rubric(what, null, examples == null ? null : List.of(examples));
    }

    /**
     * Returns a copy that also states which related cases this must not be used for.
     */
    public Rubric notFor(String notFor) {
        return new Rubric(what, notFor, examples);
    }
}
