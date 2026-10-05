package dev.monkeypatch.rctiming.domain.race;

import dev.monkeypatch.rctiming.jooq.generated.tables.records.UnknownTransponderLinksRecord;
import dev.monkeypatch.rctiming.persistence.JooqRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static dev.monkeypatch.rctiming.jooq.generated.tables.UnknownTransponderLinks.UNKNOWN_TRANSPONDER_LINKS;

@Repository
public class UnknownTransponderLinkRepository extends JooqRepository<UnknownTransponderLink, UnknownTransponderLinksRecord> {

    public UnknownTransponderLinkRepository(DSLContext dsl) {
        super(dsl, UNKNOWN_TRANSPONDER_LINKS, UNKNOWN_TRANSPONDER_LINKS.ID);
    }

    public Optional<UnknownTransponderLink> findByRaceIdAndTransponderNumber(Long raceId, String transponderNumber) {
        return findOne(UNKNOWN_TRANSPONDER_LINKS.RACE_ID.eq(raceId)
                .and(UNKNOWN_TRANSPONDER_LINKS.TRANSPONDER_NUMBER.eq(transponderNumber)));
    }

    @Override
    protected UnknownTransponderLink toEntity(UnknownTransponderLinksRecord r) {
        UnknownTransponderLink u = new UnknownTransponderLink();
        u.setId(r.getId());
        u.setRaceId(r.getRaceId());
        u.setTransponderNumber(r.getTransponderNumber());
        u.setLinkedEntryId(r.getLinkedEntryId());
        u.setLinkedBy(r.getLinkedBy());
        u.setLinkedAt(r.getLinkedAt());
        return u;
    }

    @Override
    protected void toRecord(UnknownTransponderLink u, UnknownTransponderLinksRecord r) {
        r.setRaceId(u.getRaceId());
        r.setTransponderNumber(u.getTransponderNumber());
        r.setLinkedEntryId(u.getLinkedEntryId());
        r.setLinkedBy(u.getLinkedBy());
        r.setLinkedAt(u.getLinkedAt());
    }

    @Override
    protected Long idOf(UnknownTransponderLink u) {
        return u.getId();
    }

    @Override
    protected void setId(UnknownTransponderLink u, Long id) {
        u.setId(id);
    }
}
