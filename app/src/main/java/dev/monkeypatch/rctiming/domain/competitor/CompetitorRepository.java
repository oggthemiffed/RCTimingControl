package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.jooq.generated.tables.records.CompetitorsRecord;
import dev.monkeypatch.rctiming.persistence.JooqRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Competitors.COMPETITORS;

@Repository
public class CompetitorRepository extends JooqRepository<Competitor, CompetitorsRecord> {

    public CompetitorRepository(DSLContext dsl) {
        super(dsl, COMPETITORS, COMPETITORS.ID);
    }

    public Optional<Competitor> findByExternalSourceAndExternalId(String externalSource, String externalId) {
        return findOne(COMPETITORS.EXTERNAL_SOURCE.eq(externalSource).and(COMPETITORS.EXTERNAL_ID.eq(externalId)));
    }

    @Override
    protected Competitor toEntity(CompetitorsRecord r) {
        Competitor c = new Competitor();
        c.setId(r.getId());
        c.setDisplayName(r.getDisplayName());
        c.setExternalSource(r.getExternalSource());
        c.setExternalId(r.getExternalId());
        c.setBrcaNumber(r.getBrcaNumber());
        c.setHomeClub(r.getHomeClub());
        c.setCreatedAt(r.getCreatedAt());
        c.setUpdatedAt(r.getUpdatedAt());
        return c;
    }

    @Override
    protected void toRecord(Competitor c, CompetitorsRecord r) {
        r.setDisplayName(c.getDisplayName());
        r.setExternalSource(c.getExternalSource());
        r.setExternalId(c.getExternalId());
        r.setBrcaNumber(c.getBrcaNumber());
        r.setHomeClub(c.getHomeClub());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
    }

    @Override
    protected Long idOf(Competitor c) {
        return c.getId();
    }

    @Override
    protected void setId(Competitor c, Long id) {
        c.setId(id);
    }
}
