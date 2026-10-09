package dev.monkeypatch.rctiming.query.event;

import dev.monkeypatch.rctiming.query.event.EventScheduleDto.EntryAvailability;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static dev.monkeypatch.rctiming.query.event.EventScheduleQuery.entryAvailability;
import static org.assertj.core.api.Assertions.assertThat;

class EventScheduleQueryTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final Instant EARLIER = NOW.minus(Duration.ofDays(1));
    private static final Instant LATER = NOW.plus(Duration.ofDays(1));

    @Test
    void closedOrRunningEventsAreClosedForEntry() {
        assertThat(entryAvailability("ENTRIES_CLOSED", null, null, NOW)).isEqualTo(EntryAvailability.ENTRY_CLOSED);
        assertThat(entryAvailability("IN_PROGRESS", null, null, NOW)).isEqualTo(EntryAvailability.ENTRY_CLOSED);
    }

    @Test
    void anEventPastItsClosingTimeIsClosedWhateverItsStatus() {
        assertThat(entryAvailability("OPEN", null, EARLIER, NOW)).isEqualTo(EntryAvailability.ENTRY_CLOSED);
    }

    @Test
    void anOpenEventOrAPublishedOneInsideItsWindowIsOpen() {
        assertThat(entryAvailability("OPEN", LATER, null, NOW)).isEqualTo(EntryAvailability.ENTRY_OPEN);
        assertThat(entryAvailability("PUBLISHED", EARLIER, LATER, NOW)).isEqualTo(EntryAvailability.ENTRY_OPEN);
        assertThat(entryAvailability("PUBLISHED", null, null, NOW)).isEqualTo(EntryAvailability.ENTRY_OPEN);
    }

    @Test
    void aPublishedEventBeforeItsOpeningTimeIsNotYetOpen() {
        assertThat(entryAvailability("PUBLISHED", LATER, null, NOW)).isEqualTo(EntryAvailability.ENTRY_NOT_YET_OPEN);
    }
}
