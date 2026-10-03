package dev.monkeypatch.rctiming.localday.sync.dto;

/** {@code cloudEntryId}/{@code racerName}/{@code transponderNumber} are null if the local entry
 * this result row referenced could not be resolved (e.g. reassigned or removed since). */
public record ResultRow(Long cloudEntryId, String racerName, String transponderNumber,
                         int position, int lapsCompleted, Long bestLapMs) {
}
