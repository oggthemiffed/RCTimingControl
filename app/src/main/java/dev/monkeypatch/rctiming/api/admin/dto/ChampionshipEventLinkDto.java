package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.championship.ChampionshipEventLink;

public record ChampionshipEventLinkDto(
        Long id,
        Long championshipId,
        Long eventId,
        int roundNumber
) {
    public static ChampionshipEventLinkDto from(ChampionshipEventLink l) {
        return new ChampionshipEventLinkDto(l.getId(), l.getChampionshipId(), l.getEventId(), l.getRoundNumber());
    }
}
