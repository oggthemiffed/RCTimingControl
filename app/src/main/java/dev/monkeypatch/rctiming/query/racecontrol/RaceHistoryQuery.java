package dev.monkeypatch.rctiming.query.racecontrol;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static dev.monkeypatch.rctiming.jooq.generated.tables.AuditLog.AUDIT_LOG;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Competitors.COMPETITORS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.IncidentReports.INCIDENT_REPORTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.MarshalAbsences.MARSHAL_ABSENCES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.MarshalAdjustments.MARSHAL_ADJUSTMENTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.MarshalPenalties.MARSHAL_PENALTIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Penalties.PENALTIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.UnknownTransponderLink.UNKNOWN_TRANSPONDER_LINK;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Users.USERS;

/**
 * Everything that happened in one race (#140), oldest first, so a race director can answer "what happened?".
 * The race's own tables (penalties, incidents, marshal laps, links) hold records from before the audit log
 * existed, so they are read directly; the audit log adds what has no table of its own, such as the race
 * starting, stopping and finishing. Audit rows that only repeat a race table row are left out.
 */
@Component
@Transactional(readOnly = true)
public class RaceHistoryQuery {

    /** Audit actions whose facts are already read from the race's own tables. */
    static final Set<String> COVERED_BY_TABLES = Set.of(
            "PENALTY_APPLIED", "INCIDENT_RAISED", "MARSHAL_ABSENCE_RECORDED", "MARSHAL_PENALTY_APPLIED",
            "UNKNOWN_TRANSPONDER_LINKED");

    private final DSLContext dsl;

    public RaceHistoryQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<RaceHistoryDto> forRace(long raceId) {
        List<RaceHistoryDto> all = new ArrayList<>();
        all.addAll(penalties(raceId));
        all.addAll(incidents(raceId));
        all.addAll(marshalLaps(raceId));
        all.addAll(marshalAbsences(raceId));
        all.addAll(marshalPenalties(raceId));
        all.addAll(transponderLinks(raceId));
        all.addAll(auditRows(raceId));
        all.sort(Comparator.comparing(RaceHistoryDto::at));
        return all;
    }

    private static Field<String> userName() {
        return DSL.concat(USERS.FIRST_NAME, DSL.val(" "), USERS.LAST_NAME);
    }

    private List<RaceHistoryDto> penalties(long raceId) {
        return dsl.select(PENALTIES.APPLIED_AT, userName(), COMPETITORS.DISPLAY_NAME, PENALTIES.PENALTY_TYPE,
                        PENALTIES.VALUE, PENALTIES.REASON)
                .from(PENALTIES)
                .join(ENTRIES).on(ENTRIES.ID.eq(PENALTIES.ENTRY_ID))
                .leftJoin(COMPETITORS).on(COMPETITORS.ID.eq(ENTRIES.COMPETITOR_ID))
                .leftJoin(USERS).on(USERS.ID.eq(PENALTIES.APPLIED_BY))
                .where(PENALTIES.RACE_ID.eq(raceId))
                .fetch(r -> new RaceHistoryDto(r.value1(), "PENALTY", r.value2(), r.value3(),
                        r.value4() + " penalty of " + r.value5().stripTrailingZeros().toPlainString()
                                + reasonSuffix(r.value6())));
    }

    private List<RaceHistoryDto> incidents(long raceId) {
        return dsl.select(INCIDENT_REPORTS.RAISED_AT, userName(), COMPETITORS.DISPLAY_NAME,
                        INCIDENT_REPORTS.INCIDENT_TYPE, INCIDENT_REPORTS.DESCRIPTION)
                .from(INCIDENT_REPORTS)
                .join(ENTRIES).on(ENTRIES.ID.eq(INCIDENT_REPORTS.ENTRY_ID))
                .leftJoin(COMPETITORS).on(COMPETITORS.ID.eq(ENTRIES.COMPETITOR_ID))
                .leftJoin(USERS).on(USERS.ID.eq(INCIDENT_REPORTS.RAISED_BY))
                .where(INCIDENT_REPORTS.RACE_ID.eq(raceId))
                .fetch(r -> new RaceHistoryDto(r.value1(), "INCIDENT", r.value2(), r.value3(),
                        "Incident reported: " + r.value4() + reasonSuffix(r.value5())));
    }

    private List<RaceHistoryDto> marshalLaps(long raceId) {
        return dsl.select(MARSHAL_ADJUSTMENTS.ADJUSTED_AT, MARSHAL_ADJUSTMENTS.ACTING_USER_NAME,
                        COMPETITORS.DISPLAY_NAME, MARSHAL_ADJUSTMENTS.LAP_DELTA, MARSHAL_ADJUSTMENTS.RACE_STATE_AT_TIME)
                .from(MARSHAL_ADJUSTMENTS)
                .join(ENTRIES).on(ENTRIES.ID.eq(MARSHAL_ADJUSTMENTS.ENTRY_ID))
                .leftJoin(COMPETITORS).on(COMPETITORS.ID.eq(ENTRIES.COMPETITOR_ID))
                .where(MARSHAL_ADJUSTMENTS.RACE_ID.eq(raceId))
                .fetch(r -> new RaceHistoryDto(r.value1(), "MARSHAL_LAP", r.value2(), r.value3(),
                        (r.value4() > 0 ? "Added a lap" : "Removed a lap") + " (race was " + r.value5() + ")"));
    }

    private List<RaceHistoryDto> marshalAbsences(long raceId) {
        return dsl.select(MARSHAL_ABSENCES.RECORDED_AT, userName(), COMPETITORS.DISPLAY_NAME)
                .from(MARSHAL_ABSENCES)
                .join(ENTRIES).on(ENTRIES.ID.eq(MARSHAL_ABSENCES.ENTRY_ID))
                .leftJoin(COMPETITORS).on(COMPETITORS.ID.eq(ENTRIES.COMPETITOR_ID))
                .leftJoin(USERS).on(USERS.ID.eq(MARSHAL_ABSENCES.RECORDED_BY))
                .where(MARSHAL_ABSENCES.RACE_ID.eq(raceId))
                .fetch(r -> new RaceHistoryDto(r.value1(), "MARSHAL_ABSENCE", r.value2(), r.value3(),
                        "Did not marshal"));
    }

    private List<RaceHistoryDto> marshalPenalties(long raceId) {
        return dsl.select(MARSHAL_PENALTIES.APPLIED_AT, userName(), COMPETITORS.DISPLAY_NAME, MARSHAL_PENALTIES.NOTES)
                .from(MARSHAL_PENALTIES)
                .join(MARSHAL_ABSENCES).on(MARSHAL_ABSENCES.ID.eq(MARSHAL_PENALTIES.ABSENCE_ID))
                .join(ENTRIES).on(ENTRIES.ID.eq(MARSHAL_PENALTIES.ENTRY_ID))
                .leftJoin(COMPETITORS).on(COMPETITORS.ID.eq(ENTRIES.COMPETITOR_ID))
                .leftJoin(USERS).on(USERS.ID.eq(MARSHAL_PENALTIES.APPLIED_BY))
                .where(MARSHAL_ABSENCES.RACE_ID.eq(raceId))
                .fetch(r -> new RaceHistoryDto(r.value1(), "MARSHAL_PENALTY", r.value2(), r.value3(),
                        "Marshalling penalty" + reasonSuffix(r.value4())));
    }

    private List<RaceHistoryDto> transponderLinks(long raceId) {
        return dsl.select(UNKNOWN_TRANSPONDER_LINK.LINKED_AT, userName(), COMPETITORS.DISPLAY_NAME,
                        UNKNOWN_TRANSPONDER_LINK.TRANSPONDER_NUMBER)
                .from(UNKNOWN_TRANSPONDER_LINK)
                .join(ENTRIES).on(ENTRIES.ID.eq(UNKNOWN_TRANSPONDER_LINK.ENTRY_ID))
                .leftJoin(COMPETITORS).on(COMPETITORS.ID.eq(ENTRIES.COMPETITOR_ID))
                .leftJoin(USERS).on(USERS.ID.eq(UNKNOWN_TRANSPONDER_LINK.LINKED_BY_USER_ID))
                .where(UNKNOWN_TRANSPONDER_LINK.RACE_ID.eq(raceId))
                .fetch(r -> new RaceHistoryDto(r.value1(), "TRANSPONDER_LINK", r.value2(), r.value3(),
                        "Linked transponder " + r.value4()));
    }

    private List<RaceHistoryDto> auditRows(long raceId) {
        return dsl.select(AUDIT_LOG.OCCURRED_AT, AUDIT_LOG.ACTOR_LABEL, AUDIT_LOG.ACTION, AUDIT_LOG.SUMMARY)
                .from(AUDIT_LOG)
                .where(AUDIT_LOG.RACE_ID.eq(raceId))
                .and(AUDIT_LOG.ACTION.notIn(COVERED_BY_TABLES))
                .fetch(r -> new RaceHistoryDto(r.value1(), r.value3().startsWith("RACE_") ? "LIFECYCLE" : "OTHER",
                        r.value2(), null, r.value4()));
    }

    private static String reasonSuffix(String reason) {
        return reason == null || reason.isBlank() ? "" : ": " + reason;
    }
}
