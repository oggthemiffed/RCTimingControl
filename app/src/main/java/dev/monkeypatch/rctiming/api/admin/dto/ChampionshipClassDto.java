package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.championship.ChampionshipClass;

public record ChampionshipClassDto(
        Long id,
        Long championshipId,
        Long racingClassId,
        Integer bestXFromYX,
        Integer bestXFromYY
) {
    public static ChampionshipClassDto from(ChampionshipClass c) {
        return new ChampionshipClassDto(
                c.getId(), c.getChampionshipId(), c.getRacingClassId(),
                c.getBestXFromYX(), c.getBestXFromYY());
    }
}
