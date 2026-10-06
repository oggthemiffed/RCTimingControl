package dev.monkeypatch.rctiming.api.racecontrol.dto;

import java.time.Instant;

/**
 * One entry as the check-in desk sees it (L11).
 *
 * @param racehubArrival RaceHub's arrival mark (NOT_ARRIVED / ARRIVED), read-only; null for
 *                       entries not imported from RaceHub
 * @param importedTransponderNumber the imported file's primary, when it differs from a number swapped on the
 *                                  day (#50); null otherwise
 * @param importedSecondaryTransponderNumber the same for the secondary
 */
public record CheckInEntryDto(
        long entryId,
        String competitorName,
        String className,
        String transponderNumber,
        String secondaryTransponderNumber,
        boolean checkedIn,
        Instant checkedInAt,
        String racehubArrival,
        String importedTransponderNumber,
        String importedSecondaryTransponderNumber
) {}
