package dev.monkeypatch.rctiming.query.championship;

import dev.monkeypatch.rctiming.domain.championship.ChampionshipRepository;
import jakarta.persistence.EntityNotFoundException;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CHAMP-01, CHAMP-02, CHAMP-04, CHAMP-07, CHAMP-08, CHAMP-09 read side.
 *
 * Phase 3: returns empty standings (race_results table does not yet exist).
 * Phase 7: implements full join over race_results × championship_points_scale × championship_exclusions,
 * applies best-X-from-Y drop, adds TQ and A-final-winner bonuses.
 */
@Service
@Transactional(readOnly = true)
public class ChampionshipStandingsQuery {

    @SuppressWarnings("unused")  // will be used in Phase 7
    private final DSLContext dsl;

    private final ChampionshipRepository championshipRepository;

    public ChampionshipStandingsQuery(DSLContext dsl, ChampionshipRepository championshipRepository) {
        this.dsl = dsl;
        this.championshipRepository = championshipRepository;
    }

    public List<StandingsRowDto> computeStandings(Long championshipId) {
        if (!championshipRepository.existsById(championshipId)) {
            throw new EntityNotFoundException("Championship not found: " + championshipId);
        }
        // Phase 7 TODO: join race_results × points_scale × exclusions, apply best-X drop, bonuses.
        return List.of();
    }
}
