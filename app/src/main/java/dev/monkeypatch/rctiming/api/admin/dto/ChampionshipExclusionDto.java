package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.championship.ChampionshipExclusion;

import java.time.Instant;

public record ChampionshipExclusionDto(
        Long id,
        Long championshipId,
        Long driverId,
        Long eventId,
        String reason,
        Long createdBy,
        Instant createdAt
) {
    public static ChampionshipExclusionDto from(ChampionshipExclusion x) {
        return new ChampionshipExclusionDto(
                x.getId(), x.getChampionshipId(), x.getDriverId(), x.getEventId(),
                x.getReason(), x.getCreatedBy(), x.getCreatedAt());
    }
}
