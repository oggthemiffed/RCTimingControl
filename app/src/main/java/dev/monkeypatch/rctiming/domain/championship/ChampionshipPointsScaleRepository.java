package dev.monkeypatch.rctiming.domain.championship;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.ChampionshipPointsScale.CHAMPIONSHIP_POINTS_SCALE;

/**
 * A championship's points for each finishing position. Keyed by championship and position rather
 * than an id, so it doesn't extend {@code JooqRepository}.
 */
@Repository
public class ChampionshipPointsScaleRepository {

    private final DSLContext dsl;

    public ChampionshipPointsScaleRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<ChampionshipPointsScaleEntry> findByChampionshipIdOrderByPositionAsc(Long championshipId) {
        return dsl.selectFrom(CHAMPIONSHIP_POINTS_SCALE)
                .where(CHAMPIONSHIP_POINTS_SCALE.CHAMPIONSHIP_ID.eq(championshipId))
                .orderBy(CHAMPIONSHIP_POINTS_SCALE.POSITION.asc())
                .fetch(r -> new ChampionshipPointsScaleEntry(r.getChampionshipId(), r.getPosition(), r.getPoints()));
    }

    /** Inserts the entry, or replaces the points of the one already at its position. */
    @Transactional
    public ChampionshipPointsScaleEntry save(ChampionshipPointsScaleEntry entry) {
        dsl.insertInto(CHAMPIONSHIP_POINTS_SCALE)
                .set(CHAMPIONSHIP_POINTS_SCALE.CHAMPIONSHIP_ID, entry.getChampionshipId())
                .set(CHAMPIONSHIP_POINTS_SCALE.POSITION, entry.getPosition())
                .set(CHAMPIONSHIP_POINTS_SCALE.POINTS, entry.getPoints())
                .onConflict(CHAMPIONSHIP_POINTS_SCALE.CHAMPIONSHIP_ID, CHAMPIONSHIP_POINTS_SCALE.POSITION)
                .doUpdate()
                .set(CHAMPIONSHIP_POINTS_SCALE.POINTS, entry.getPoints())
                .execute();
        return entry;
    }

    @Transactional
    public List<ChampionshipPointsScaleEntry> saveAll(Iterable<ChampionshipPointsScaleEntry> entries) {
        List<ChampionshipPointsScaleEntry> saved = new ArrayList<>();
        entries.forEach(entry -> saved.add(save(entry)));
        return saved;
    }

    @Transactional
    public int deleteAllByChampionshipId(Long championshipId) {
        return dsl.deleteFrom(CHAMPIONSHIP_POINTS_SCALE)
                .where(CHAMPIONSHIP_POINTS_SCALE.CHAMPIONSHIP_ID.eq(championshipId))
                .execute();
    }
}
