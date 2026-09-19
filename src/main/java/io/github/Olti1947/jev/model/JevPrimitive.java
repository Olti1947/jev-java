package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Choice.class, name = "choice"),
        @JsonSubTypes.Type(value = Score.class, name = "score"),
        @JsonSubTypes.Type(value = Noul.class, name = "noul")
})
public sealed interface JevPrimitive permits Choice, Score, Noul {
    String name();

    /**
     * The instructions for this question: either a plain {@link String} or a
     * structured value (typically a {@link java.util.Map} with keys such as
     * {@code question}, {@code focus}, {@code inspect}, {@code field}).
     */
    Object instructions();
}
