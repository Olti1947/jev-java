package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * One answer in a Jev response, keyed by the question name it answers.
 * The concrete type mirrors the primitive that asked the question.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoulAnswer.class, name = "noul"),
        @JsonSubTypes.Type(value = ChoiceAnswer.class, name = "choice"),
        @JsonSubTypes.Type(value = ScoreAnswer.class, name = "score")
})
public sealed interface JevAnswer permits NoulAnswer, ChoiceAnswer, ScoreAnswer {
}
