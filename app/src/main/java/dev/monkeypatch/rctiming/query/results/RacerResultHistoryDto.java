package dev.monkeypatch.rctiming.query.results;

import java.time.LocalDate;
import java.util.List;

/**
 * Per-event result history for the racer portal Results tab.
 * Sorted by eventDate DESC at query time.
 *
 * IDOR safety: userId in {@link RacerResultHistoryQuery#findForUser(Long)} must always
 * originate from the JWT principal in the controller — never from a caller-supplied parameter.
 */
public record RacerResultHistoryDto(
        Long eventId,
        String eventName,
        LocalDate eventDate,
        List<RaceResult> races
) {
    /**
     * One result row per race the racer was entered in at this event.
     */
    public record RaceResult(
            Long raceId,
            String raceLabel,     // e.g. "Class A - Q1" or "Class A - A Final"
            int position,         // 0 if race is not finished / no snapshot
            int lapsCompleted,
            Long bestLapMs        // null if no laps completed
    ) {}
}
