package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.championship.Championship;
import dev.monkeypatch.rctiming.domain.championship.ScoringSource;

import java.util.List;

public record ChampionshipDetailDto(
        Long id,
        String name,
        Integer bestXFromYX,
        Integer bestXFromYY,
        ScoringSource scoringSource,
        int tqBonusPoints,
        int afinalWinnerBonusPoints,
        List<ChampionshipClassDto> classes,
        List<ChampionshipEventLinkDto> events,
        List<PointsScaleEntryDto> pointsScale
) {
    public static ChampionshipDetailDto from(
            Championship c,
            List<ChampionshipClassDto> classes,
            List<ChampionshipEventLinkDto> events,
            List<PointsScaleEntryDto> pointsScale) {
        return new ChampionshipDetailDto(
                c.getId(), c.getName(),
                c.getBestXFromYX(), c.getBestXFromYY(),
                c.getScoringSource(),
                c.getTqBonusPoints(), c.getAfinalWinnerBonusPoints(),
                classes, events, pointsScale);
    }
}
