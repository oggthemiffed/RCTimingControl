package dev.monkeypatch.rctiming.api.racecontrol.dto;

import java.time.Instant;

/**
 * One entry as the check-in desk sees it (L11).
 *
 * @param racehubArrival RaceHub's arrival mark (NOT_ARRIVED / ARRIVED), read-only; null for
 *                       entries not imported from RaceHub
 */
public record CheckInEntryDto(
        long entryId,
        String competitorName,
        String className,
        String transponderNumber,
        String secondaryTransponderNumber,
        boolean checkedIn,
        Instant checkedInAt,
        String racehubArrival
) {}
