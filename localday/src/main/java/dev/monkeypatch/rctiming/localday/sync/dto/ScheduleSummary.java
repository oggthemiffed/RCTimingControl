package dev.monkeypatch.rctiming.localday.sync.dto;

/** Identifies a race/heat by its cloud-side id, not this instance's local row id. */
public record ScheduleSummary(Long cloudRaceId, int roundNumber, int heatNumber, String className,
                               String finalLetter, String status) {
}
