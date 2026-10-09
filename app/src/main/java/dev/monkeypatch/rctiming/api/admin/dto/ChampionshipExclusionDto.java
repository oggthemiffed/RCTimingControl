package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.championship.ChampionshipExclusion;
import dev.monkeypatch.rctiming.domain.championship.ChampionshipService;

import java.time.Instant;

public record ChampionshipExclusionDto(
        Long id,
        Long championshipId,
        Long driverId,
        Long eventId,
        String reason,
        Long createdBy,
        /** The official who recorded it, by name; null if that account has since been removed. */
        String createdByName,
        Instant createdAt
) {
    public static ChampionshipExclusionDto from(ChampionshipService.Exclusion exclusion) {
        return from(exclusion.exclusion(), exclusion.recordedByName());
    }

    public static ChampionshipExclusionDto from(ChampionshipExclusion x, String createdByName) {
        return new ChampionshipExclusionDto(
                x.getId(), x.getChampionshipId(), x.getDriverId(), x.getEventId(),
                x.getReason(), x.getCreatedBy(), createdByName, x.getCreatedAt());
    }
}
