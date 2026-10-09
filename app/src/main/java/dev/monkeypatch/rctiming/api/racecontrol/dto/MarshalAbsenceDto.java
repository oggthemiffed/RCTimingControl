package dev.monkeypatch.rctiming.api.racecontrol.dto;

import dev.monkeypatch.rctiming.domain.race.MarshalAbsence;

import java.time.Instant;

/** A recorded marshal absence; its id is what a marshal penalty names. */
public record MarshalAbsenceDto(Long id, Long raceId, Long entryId, Long eventId, Instant recordedAt,
                                Long recordedBy) {

    public static MarshalAbsenceDto from(MarshalAbsence a) {
        return new MarshalAbsenceDto(a.getId(), a.getRaceId(), a.getEntryId(), a.getEventId(), a.getRecordedAt(),
                a.getRecordedBy());
    }
}
