package dev.monkeypatch.rctiming.domain.format;

import dev.monkeypatch.rctiming.jooq.generated.tables.records.EventClassesRecord;
import dev.monkeypatch.rctiming.persistence.JooqRepository;
import dev.monkeypatch.rctiming.persistence.convert.JsonMapConverter;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;

@Repository
public class EventClassRepository extends JooqRepository<EventClass, EventClassesRecord> {

    private static final RaceFormatConfigConverter CONFIG = new RaceFormatConfigConverter();
    private static final JsonMapConverter OVERRIDE = new JsonMapConverter();

    public EventClassRepository(DSLContext dsl) {
        super(dsl, EVENT_CLASSES, EVENT_CLASSES.ID);
    }

    public List<EventClass> findByEventId(Long eventId) {
        return findWhere(EVENT_CLASSES.EVENT_ID.eq(eventId));
    }

    /** Ids and racing classes of an event's classes, without loading their format config. */
    public List<EventClassRef> findRefsByEventId(Long eventId) {
        return dsl.select(EVENT_CLASSES.ID, EVENT_CLASSES.RACING_CLASS_ID)
                .from(EVENT_CLASSES)
                .where(EVENT_CLASSES.EVENT_ID.eq(eventId))
                .orderBy(EVENT_CLASSES.ID)
                .fetch(row -> new EventClassRef(row.value1(), row.value2()));
    }

    public record EventClassRef(Long id, Long racingClassId) {
        public Long getId() {
            return id;
        }

        public Long getRacingClassId() {
            return racingClassId;
        }
    }

    @Override
    protected EventClass toEntity(EventClassesRecord r) {
        EventClass ec = new EventClass();
        ec.setId(r.getId());
        ec.setConfigSnapshot(CONFIG.convertToEntityAttribute(r.getConfigSnapshot()));
        ec.setConfigOverride(OVERRIDE.convertToEntityAttribute(r.getConfigOverride()));
        ec.setTemplateId(r.getTemplateId());
        ec.setCreatedAt(r.getCreatedAt());
        ec.setUpdatedAt(r.getUpdatedAt());
        ec.setEventId(r.getEventId());
        ec.setRacingClassId(r.getRacingClassId());
        ec.setCombinedRaceGroup(r.getCombinedRaceGroup());
        ec.setFinalsCount(r.getFinalsCount());
        ec.setCarsPerFinal(r.getCarsPerFinal());
        ec.setBumpCount(r.getBumpCount());
        return ec;
    }

    @Override
    protected void toRecord(EventClass ec, EventClassesRecord r) {
        r.setConfigSnapshot(CONFIG.convertToDatabaseColumn(ec.getConfigSnapshot()));
        r.setConfigOverride(OVERRIDE.convertToDatabaseColumn(ec.getConfigOverride()));
        r.setTemplateId(ec.getTemplateId());
        r.setCreatedAt(ec.getCreatedAt());
        r.setUpdatedAt(ec.getUpdatedAt());
        r.setEventId(ec.getEventId());
        r.setRacingClassId(ec.getRacingClassId());
        r.setCombinedRaceGroup(ec.getCombinedRaceGroup());
        r.setFinalsCount(ec.getFinalsCount());
        r.setCarsPerFinal(ec.getCarsPerFinal());
        r.setBumpCount(ec.getBumpCount());
    }

    /** The creation time is set once, on insert. */
    @Override
    protected List<Field<?>> insertOnly() {
        return List.of(EVENT_CLASSES.CREATED_AT);
    }

    @Override
    protected Long idOf(EventClass ec) {
        return ec.getId();
    }

    @Override
    protected void setId(EventClass ec, Long id) {
        ec.setId(id);
    }
}
