package dev.monkeypatch.rctiming.domain.race;

import dev.monkeypatch.rctiming.jooq.generated.tables.records.MarshalPenaltiesRecord;
import dev.monkeypatch.rctiming.persistence.JooqRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static dev.monkeypatch.rctiming.jooq.generated.tables.MarshalPenalties.MARSHAL_PENALTIES;

@Repository
public class MarshalPenaltyRepository extends JooqRepository<MarshalPenalty, MarshalPenaltiesRecord> {

    public MarshalPenaltyRepository(DSLContext dsl) {
        super(dsl, MARSHAL_PENALTIES, MARSHAL_PENALTIES.ID);
    }

    @Override
    protected MarshalPenalty toEntity(MarshalPenaltiesRecord r) {
        MarshalPenalty m = new MarshalPenalty();
        m.setId(r.getId());
        m.setAbsenceId(r.getAbsenceId());
        m.setEntryId(r.getEntryId());
        m.setEventId(r.getEventId());
        m.setAppliedBy(r.getAppliedBy());
        m.setAppliedAt(r.getAppliedAt());
        m.setNotes(r.getNotes());
        return m;
    }

    @Override
    protected void toRecord(MarshalPenalty m, MarshalPenaltiesRecord r) {
        r.setAbsenceId(m.getAbsenceId());
        r.setEntryId(m.getEntryId());
        r.setEventId(m.getEventId());
        r.setAppliedBy(m.getAppliedBy());
        r.setAppliedAt(m.getAppliedAt());
        r.setNotes(m.getNotes());
    }

    @Override
    protected Long idOf(MarshalPenalty m) {
        return m.getId();
    }

    @Override
    protected void setId(MarshalPenalty m, Long id) {
        m.setId(id);
    }
}
