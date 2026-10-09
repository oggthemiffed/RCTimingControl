package dev.monkeypatch.rctiming.domain.entryimport;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinalStateTest {

    private final EventClasses classes = new EventClasses(Map.of("RACEHUB-1", 10L), List.of(10L, 20L),
            Map.of("stockbuggy", List.of(10L), "touring", List.of(20L, 21L)), Map.of(10L, "Stock Buggy", 20L, "Touring"));

    @Test
    void aDriverTwiceInOneClassIsAnError() {
        FinalState state = new FinalState(classes);
        state.add("c:1", "Jane Doe", 10L, "111", null);
        state.add("c:1", "Jane Doe", 10L, "222", null);
        state.add("c:1", "Jane Doe", 20L, "333", null);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        state.check(errors, warnings);

        assertThat(errors).containsExactly("Jane Doe would have 2 entries in Stock Buggy");
        assertThat(warnings).isEmpty();
    }

    @Test
    void aSharedTransponderIsOnlyAWarningAndAnEntryUsingItTwiceCountsOnce() {
        FinalState state = new FinalState(classes);
        state.add("c:1", "Jane Doe", 10L, "111", "111");
        state.add("c:2", "John Smith", 20L, "222", "111");
        state.add("c:3", "Solo Driver", 20L, "333", "333");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        state.check(errors, warnings);

        assertThat(errors).isEmpty();
        assertThat(warnings).containsExactly("Transponder 111 is used by more than one entry: Jane Doe, John Smith");
    }

    @Test
    void eventClassesPlaceByMappingNameOrPosition() {
        assertThat(classes.mapped("RACEHUB-1")).contains(10L);
        assertThat(classes.mapped("RACEHUB-2")).isEmpty();
        assertThat(classes.byName(" Stock  Buggy ")).contains(10L);
        assertThat(classes.byName("Touring")).as("two classes share the name").isEmpty();
        assertThat(classes.atPosition(2)).contains(20L);
        assertThat(classes.atPosition(3)).isEmpty();
        assertThat(classes.name(30L)).isEqualTo("event class 30");
    }
}
