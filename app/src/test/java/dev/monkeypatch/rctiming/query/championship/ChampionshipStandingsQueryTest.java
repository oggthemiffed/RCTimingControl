package dev.monkeypatch.rctiming.query.championship;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unit/integration tests for ChampionshipStandingsQuery (Phase 7, Plan 03).
 * Tests: best-X-from-Y marks correct rounds as dropped (CHAMP-05);
 *        TQ bonus and A-final winner bonus applied correctly (CHAMP-07/08);
 *        DNS driver (entered but absent from positions_json) scores 0 and round counts toward Y.
 */
@Disabled("Stub — enabled in 07-03-PLAN.md when ChampionshipStandingsQuery.computeStandings() is implemented")
class ChampionshipStandingsQueryTest extends AbstractIntegrationTest {

    @Autowired
    ChampionshipStandingsQuery query;

    @Test
    void bestXFromYDropsWorstRounds() {
        // TODO (07-03-PLAN.md): driver with 4 rounds, best 3 from 4 — worst round marked dropped=true
        org.junit.jupiter.api.Assertions.fail("Not yet implemented");
    }

    @Test
    void tqBonusApplied() {
        // TODO (07-03-PLAN.md): fastest qualifier gets tq_bonus_points added to their total
        org.junit.jupiter.api.Assertions.fail("Not yet implemented");
    }

    @Test
    void afinalWinnerBonusApplied() {
        // TODO (07-03-PLAN.md): A-final position-1 driver gets afinal_winner_bonus_points
        org.junit.jupiter.api.Assertions.fail("Not yet implemented");
    }

    @Test
    void dnsDriverScoresZeroAndRoundCountsTowardY() {
        // TODO (07-03-PLAN.md): driver in race_entries but absent from positions_json = DNS = 0pts, round counts
        org.junit.jupiter.api.Assertions.fail("Not yet implemented");
    }
}
