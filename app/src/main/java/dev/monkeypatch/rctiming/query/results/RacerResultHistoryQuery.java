package dev.monkeypatch.rctiming.query.results;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RESULT-03 read side: returns result history for a given racer.
 *
 * Phase 7 Plan 05: implements full query joining race_results with
 * championship and event data, filtered strictly by authenticated user
 * to prevent IDOR (T-7-02).
 *
 * This stub exists so ChampionshipStandingsQueryTest compiles in Phase 7 Plan 01 (Wave 0).
 * Implementation is added in 07-05-PLAN.md.
 */
@Service
@Transactional(readOnly = true)
public class RacerResultHistoryQuery {

    @SuppressWarnings("unused")  // will be used in Phase 7 Plan 05
    private final DSLContext dsl;

    public RacerResultHistoryQuery(DSLContext dsl) {
        this.dsl = dsl;
    }
}
