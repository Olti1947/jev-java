package io.github.Olti1947.jev.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Reads one {@link ScoreAnswer} legend entry. The API echoes each level back as
 * it was sent, so an entry is either a string or a structured level; the latter
 * is reduced to its description (see {@link ScoreLevel#describe(Object)}).
 */
final class LegendEntryDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return ScoreLevel.describe(context.readValue(parser, Object.class));
    }
}
