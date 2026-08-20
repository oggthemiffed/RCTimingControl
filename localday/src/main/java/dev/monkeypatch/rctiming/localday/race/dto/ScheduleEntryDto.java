package dev.monkeypatch.rctiming.localday.race.dto;

import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;

import java.time.Instant;

/**
 * Read shape for one race/heat row from the local schedule cache, plus its current
 * {@link dev.monkeypatch.rctiming.localday.race.RaceState}.
 */
public record ScheduleEntryDto(Long id, Long cloudRaceId, int roundNumber, int heatNumber, int sequence,
                                String className, String finalLetter, Instant scheduledStartAt, String status) {

    public static ScheduleEntryDto from(CachedScheduleEntry e) {
        return new ScheduleEntryDto(e.getId(), e.getCloudRaceId(), e.getRoundNumber(), e.getHeatNumber(),
                e.getSequence(), e.getClassName(), e.getFinalLetter(), e.getScheduledStartAt(),
                e.getStatus().name());
    }
}
