package io.github.Olti1947.jev.model;

import io.github.Olti1947.jev.exception.JevValidationException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChoiceTest {

    @Test
    void rejectsBlankName() {
        assertThrows(JevValidationException.class, () ->
                new Choice(" ", "Which team?", List.of("billing", "support")));
    }

    @Test
    void rejectsNullOrEmptyCriteria() {
        assertThrows(JevValidationException.class, () ->
                new Choice("department", "Which team?", (Map<String, ?>) null));
        assertThrows(JevValidationException.class, () ->
                new Choice("department", "Which team?", Map.of()));
    }

    @Test
    void rejectsBlankOptionNames() {
        assertThrows(JevValidationException.class, () ->
                new Choice("department", "Which team?", Map.of(" ", "Blank option")));
        assertThrows(JevValidationException.class, () ->
                new Choice("department", "Which team?", List.of("billing", "")));
    }

    @Test
    void rejectsNullOptionNames() {
        Map<String, String> criteria = new HashMap<>();
        criteria.put(null, "No name");
        assertThrows(JevValidationException.class, () ->
                new Choice("department", "Which team?", criteria));
        assertThrows(JevValidationException.class, () ->
                new Choice("department", "Which team?", Arrays.asList("billing", null)));
    }

    @Test
    void rejectsDuplicateOptionsInList() {
        // Options become map keys, so a duplicate would silently collapse into one.
        assertThrows(JevValidationException.class, () ->
                new Choice("department", "Which team?", List.of("billing", "billing")));
    }

    @Test
    void enforcesMaximumOptionCount() {
        List<String> max = options(Choice.MAX_OPTIONS);
        assertDoesNotThrow(() -> new Choice("department", "Which team?", max));

        List<String> tooMany = options(Choice.MAX_OPTIONS + 1);
        assertThrows(JevValidationException.class, () ->
                new Choice("department", "Which team?", tooMany));
    }

    @Test
    void copiesCriteriaDefensivelyAndKeepsOrder() {
        Map<String, String> criteria = new LinkedHashMap<>();
        criteria.put("support", "Bugs and how-to questions");
        criteria.put("billing", null);
        Choice choice = new Choice("department", "Which team?", criteria);

        criteria.put(" ", "Added after construction");

        assertEquals(List.of("support", "billing"), List.copyOf(choice.criteria().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> choice.criteria().clear());
    }

    private static List<String> options(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> "option " + i)
                .toList();
    }
}
