package dev.monkeypatch.rctiming.query.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EventScheduleDto(
        Long id,
        String name,
        LocalDate eventDate,
        EntryAvailability entryAvailability,
        List<Long> finishedRaceIds,  // IDs of FINISHED races at this event; empty list if none
        Long championshipId,         // null if event is not linked to a championship
        // R15/R11: null for an event never run via the Local Race Day Program — these three are
        // only meaningful once event_offline_locks/event_snapshot_state have a row for this event.
        Instant lastSyncedAt,        // when the most recent accepted snapshot arrived; null if never synced
        boolean syncDelayed,         // true if lastSyncedAt is stale (R15's "may be delayed" indicator)
        boolean incompleteData       // R17: permanently true once a device-loss declaration was made
) {

    public enum EntryAvailability {
        ENTRY_OPEN,
        ENTRY_NOT_YET_OPEN,
        ENTRY_CLOSED
    }
}
