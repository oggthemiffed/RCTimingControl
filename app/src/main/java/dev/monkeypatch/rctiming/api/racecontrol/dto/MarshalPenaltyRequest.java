package dev.monkeypatch.rctiming.api.racecontrol.dto;

import jakarta.validation.constraints.NotNull;

/**
 * @param absenceId the recorded absence the penalty is for; when left out, the entry's most recent absence in
 *                  the event is used, or none if the entry has not been recorded absent
 */
public record MarshalPenaltyRequest(
        @NotNull Long entryId,
        @NotNull Long eventId,
        Long absenceId,
        String notes
) {}
