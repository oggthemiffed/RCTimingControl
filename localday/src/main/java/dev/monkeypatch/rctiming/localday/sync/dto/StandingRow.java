package dev.monkeypatch.rctiming.localday.sync.dto;

/** {@code bestPosition} is the lowest (best) finishing position this entry has recorded across
 * the class's finished races so far today; {@code racesCompleted} is how many it appeared in. */
public record StandingRow(Long cloudEntryId, String racerName, int bestPosition, int racesCompleted) {
}
