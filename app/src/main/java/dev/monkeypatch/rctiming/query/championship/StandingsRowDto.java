package dev.monkeypatch.rctiming.query.championship;

import java.util.List;

/**
 * One row of championship standings per driver-within-class.
 * Phase 3: empty list returned. Phase 7 populates after race_results aggregation is built.
 *
 * @param totalPoints sum of best-X rounds plus any bonuses (CHAMP-01, CHAMP-07, CHAMP-08)
 * @param rounds      per-round detail — position, points, excluded (CHAMP-02/09), dropped (CHAMP-01 worst-round drop)
 */
public record StandingsRowDto(
        Long driverId,
        String firstName,
        String lastName,
        Long racingClassId,
        int totalPoints,
        List<RoundResultDto> rounds
) {}
