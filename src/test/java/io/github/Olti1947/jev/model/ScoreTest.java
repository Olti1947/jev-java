package io.github.Olti1947.jev.model;

import io.github.Olti1947.jev.exception.JevValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreTest {

    @Test
    void acceptsOrderedLevels() {
        Score score = new Score("severity", "How severe is the issue?",
                List.of("cosmetic", "degraded", "blocking"));
        assertEquals(3, score.criteria().size());
        assertEquals("cosmetic", score.criteria().get(0));
    }

    @Test
    void acceptsVarargsLevels() {
        Score score = new Score("severity", "How severe?", "low", "high");
        assertEquals(List.of("low", "high"), score.criteria());
    }

    @Test
    void rejectsNullCriteria() {
        assertThrows(JevValidationException.class, () ->
                new Score("severity", "How severe?", (List<String>) null));
    }

    @Test
    void rejectsFewerThanTwoLevels() {
        assertThrows(JevValidationException.class, () ->
                new Score("severity", "How severe?", List.of("only one")));
    }

    @Test
    void rejectsMoreThanTenLevels() {
        List<String> levels = IntStream.rangeClosed(1, 11)
                .mapToObj(i -> "level " + i)
                .toList();
        assertThrows(JevValidationException.class, () ->
                new Score("severity", "How severe?", levels));
    }

    @Test
    void rejectsBlankLevel() {
        assertThrows(JevValidationException.class, () ->
                new Score("severity", "How severe?", "low", " "));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(JevValidationException.class, () ->
                new Score(" ", "How severe?", "low", "high"));
    }
}
