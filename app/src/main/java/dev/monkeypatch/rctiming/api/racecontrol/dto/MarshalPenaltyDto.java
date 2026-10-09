package dev.monkeypatch.rctiming.api.racecontrol.dto;

import dev.monkeypatch.rctiming.domain.race.MarshalPenalty;

import java.time.Instant;

/** A marshal penalty, and the absence it was given for when there is one. */
public record MarshalPenaltyDto(Long id, Long absenceId, Long entryId, Long eventId, Long appliedBy,
                                Instant appliedAt, String notes) {

    public static MarshalPenaltyDto from(MarshalPenalty p) {
        return new MarshalPenaltyDto(p.getId(), p.getAbsenceId(), p.getEntryId(), p.getEventId(), p.getAppliedBy(),
                p.getAppliedAt(), p.getNotes());
    }
}
