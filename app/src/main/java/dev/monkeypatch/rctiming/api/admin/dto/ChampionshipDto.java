package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.championship.Championship;
import dev.monkeypatch.rctiming.domain.championship.ScoringSource;

public record ChampionshipDto(
        Long id,
        String name,
        Integer bestXFromYX,
        Integer bestXFromYY,
        ScoringSource scoringSource,
        int tqBonusPoints,
        int afinalWinnerBonusPoints
) {
    public static ChampionshipDto from(Championship c) {
        return new ChampionshipDto(
                c.getId(), c.getName(),
                c.getBestXFromYX(), c.getBestXFromYY(),
                c.getScoringSource(),
                c.getTqBonusPoints(), c.getAfinalWinnerBonusPoints()
        );
    }
}
